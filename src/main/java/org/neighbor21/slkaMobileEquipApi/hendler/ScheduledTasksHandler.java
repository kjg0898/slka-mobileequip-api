package org.neighbor21.slkaMobileEquipApi.handler;

import jakarta.annotation.PostConstruct;
import kong.unirest.UnirestException;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.service.MCATLYSTApiService;
import org.neighbor21.slkaMobileEquipApi.service.conversion.SiteService;
import org.neighbor21.slkaMobileEquipApi.service.conversion.SurveyPeriodService;
import org.neighbor21.slkaMobileEquipApi.service.conversion.VehiclePassService;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.handler
 * fileName       : ScheduledTasksHandler.java
 * author         : kjg08
 * date           : 24. 4. 22.
 * description    : API 데이터를 가져오는 예약된 작업을 처리하는 핸들러 클래스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 22.        kjg08           최초 생성
 * 24. 5. 17.        kjg08            주석 추가 및 description 갱신
 */
@Component
public class ScheduledTasksHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksHandler.class);

    // 마지막 차량 통과 시간 파일 저장 매니저
    private final VehicleUtils.LastVehiclePassTimeManager lastVehiclePassTimeManager = new VehicleUtils.LastVehiclePassTimeManager();
    private long totalListSitesProcessTime = 0;
    private long totalIndividualVehiclesProcessTime = 0;

    @Autowired
    private MCATLYSTApiService mcAtlystApiService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private VehiclePassService vehiclePassService;

    @Autowired
    private SurveyPeriodService surveyPeriodService;

    /**
     * 초기화 메서드로, 마지막 차량 통과 시간을 로드한다.
     */
    @PostConstruct
    public void init() {
        lastVehiclePassTimeManager.loadLastVehiclePassTimes();
    }

    /**
     * 1시간 마다 List Sites 장소 목록을 호출하여 TL_MVMNEQ_CUR 테이블에 이동형 장비 설치위치 관리를 업데이트 한다.
     */
    @Scheduled(cron = "${scheduler.cron.listSites}") // TL_RIS_ROADWIDTH api 호출
    public void fetchAndCacheListSite() {
        long processStartTime = System.currentTimeMillis();
        try {
            long fetchStartTime = System.currentTimeMillis();
            List<ListSiteDTO> listSites = mcAtlystApiService.listSites(); // 수집
            long fetchEndTime = System.currentTimeMillis();
            logger.info("listSites API 호출에 걸린 총 시간: {} ms", (fetchEndTime - fetchStartTime));

            listSites.forEach(listSite -> mcAtlystApiService.cacheSite(listSite.getSite_id()));

            // 저장
            if (!listSites.isEmpty()) {
                siteService.saveSiteLogs(listSites);
                surveyPeriodService.saveSurveyPeriods(listSites);
            }
        } catch (UnirestException e) {
            handleApiException("Failed to fetch and cache list sites", e);
        } finally {
            long processEndTime = System.currentTimeMillis();
            long processTime = processEndTime - processStartTime;
            totalListSitesProcessTime = processTime;
            logger.info("listSites api --> db 적재까지 전체 실행 시간: {} ms", processTime);
        }
    }

    /**
     * 5분 간격으로 Individual Vehicles 개별 차량 호출 후 데이터를 처리한다.
     */
    @Scheduled(cron = "${scheduler.cron.IndividualVehicles}") // TL_RIS_ROADWIDTH api 호출
    public void fetchIndividualVehicles() {
        long processStartTime = System.currentTimeMillis();
        List<Integer> processedSiteIds = new ArrayList<>();
        totalIndividualVehiclesProcessTime = 0; // 초기화

        if (mcAtlystApiService.isCacheEmpty()) {
            logger.info("Site cache is empty, skipping fetchIndividualVehicles");
            return;
        }
        Set<Integer> siteCache = mcAtlystApiService.getSiteCache();
        int batchSize = Constants.DEFAULT_BATCH_SIZE; // 배치 크기 정의
        List<Integer> siteList = new ArrayList<>(siteCache);

        // Site ID 배치 사이즈 별로 모아서 처리
        for (int i = 0; i < siteList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, siteList.size());
            List<Integer> batchList = siteList.subList(i, end); // 배치 개수만큼 리스트에 담음

            // 각 배치 리스트의 사이트 ID를 처리
            List<IndividualVehiclesDTO> allVehicles = new ArrayList<>();
            for (Integer siteId : batchList) {
                allVehicles.addAll(fetchVehiclesForSite(siteId)); // 배치 개수만큼 담아서
                processedSiteIds.add(siteId);
            }

            // 배치로 차량 데이터를 저장
            if (!allVehicles.isEmpty()) {
                vehiclePassService.saveVehiclePasses(allVehicles); // 한 번에 배치 처리로 보냄
            }
        }

        // 모든 작업이 완료된 후 마지막 차량 통과 시간을 파일에 저장
        VehicleUtils.LastVehiclePassTimeManager.saveLastVehiclePassTimes();
        long processEndTime = System.currentTimeMillis();

        logger.info("IndividualVehicles api --> db 적재까지 전체 실행 시간: {} ms", processEndTime - processStartTime);
        logger.info("Total IndividualVehicles api 수집 총 시간: {} ms", totalIndividualVehiclesProcessTime);
        logger.info("Processed site IDs: {}", processedSiteIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    // 각 사이트에 대해 차량 데이터를 가져오는 메서드
    private List<IndividualVehiclesDTO> fetchVehiclesForSite(Integer siteId) {
        List<IndividualVehiclesDTO> vehicles = new ArrayList<>();
        try {
            long fetchStartTime = System.currentTimeMillis();
            vehicles = mcAtlystApiService.individualVehicles(siteId); // API 호출
            long fetchEndTime = System.currentTimeMillis();

            totalIndividualVehiclesProcessTime += (fetchEndTime - fetchStartTime);

        } catch (UnirestException e) {
            handleApiException(String.format("Failed to fetch individual vehicles for siteId: %s", siteId), e);
        }
        return vehicles;
    }

    /**
     * API 호출 중 발생한 예외를 처리하는 메서드.
     *
     * @param message 예외 메시지
     * @param e       발생한 예외
     */
    private void handleApiException(String message, Exception e) {
        logger.error(message, e);
    }
}
