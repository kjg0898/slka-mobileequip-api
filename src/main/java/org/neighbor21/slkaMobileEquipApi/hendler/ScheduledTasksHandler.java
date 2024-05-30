package org.neighbor21.slkaMobileEquipApi.hendler;

import jakarta.annotation.PostConstruct;
import kong.unirest.UnirestException;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.hendler
 * fileName       : ScheduledTasksHendler.java
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
    private long totalProcessTime = 0;
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

            //저장
            if (!listSites.isEmpty()) {
                siteService.saveSiteLogs(listSites);
                surveyPeriodService.saveSurveyPeriods(listSites);
            }
        } catch (UnirestException e) {
            handleApiException("Failed to fetch and cache list sites", e);
        } finally {
            long processEndTime = System.currentTimeMillis();
            long processTime = processEndTime - processStartTime;
            totalProcessTime += processTime;
            logger.info("listSites api --> db 적재 까지 전체 실행 시간: {} ms", processTime);
        }
    }

    /**
     * 5분 간격으로 Individual Vehicles 개별 차량 호출 후 데이터를 처리한다.
     */
    @Scheduled(cron = "${scheduler.cron.IndividualVehicles}") // TL_RIS_ROADWIDTH api 호출
    public void fetchIndividualVehicles() {
        if (mcAtlystApiService.isCacheEmpty()) {
            logger.info("Site cache is empty, skipping fetchIndividualVehicles");
            return;
        }

        long processStartTime = System.currentTimeMillis();
        Set<Integer> siteCache = mcAtlystApiService.getSiteCache();

        List<CompletableFuture<Void>> futures = siteCache.stream()
                .map(siteId -> CompletableFuture.runAsync(() -> processSite(siteId)))
                .toList();

        // Wait for all tasks to complete (포크조인)
        //CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long processEndTime = System.currentTimeMillis();
        long processTime = processEndTime - processStartTime;
        totalProcessTime += processTime;
        logger.info("fetchIndividualVehicles 메서드의 전체 실행 시간: {} ms", processTime);

        // 모든 작업이 완료된 후 마지막 차량 통과 시간을 파일에 저장
        VehicleUtils.LastVehiclePassTimeManager.saveLastVehiclePassTimes();
    }

    private void processSite(Integer siteId) {
        try {
            long fetchStartTime = System.currentTimeMillis();
            List<IndividualVehiclesDTO> vehicles = mcAtlystApiService.individualVehicles(siteId);
            long fetchEndTime = System.currentTimeMillis();
            logger.info(" IndividualVehicle API 수집 총 시간 (siteId: {}): {} ms", siteId, (fetchEndTime - fetchStartTime));

            if (!vehicles.isEmpty()) {
                vehiclePassService.saveVehiclePasses(vehicles);
            }
        } catch (UnirestException e) {
            handleApiException(String.format("Failed to fetch individual vehicles for siteId: %s", siteId), e);
        }
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
