package org.neighbor21.slkaMobileEquipApi.hendler;

import io.github.resilience4j.retry.Retry;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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
    @Autowired
    private VehicleUtils.LastVehiclePassTimeManager lastVehiclePassTimeManager;
    @Autowired
    private ExecutorService executorService;

    private long totalListSitesProcessTime = 0;
    private int totalListSitesProcessed = 0;
    private long totalIndividualVehiclesProcessTime = 0;
    private int totalIndividualVehiclesProcessed = 0;

    @Autowired
    private MCATLYSTApiService mcAtlystApiService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private VehiclePassService vehiclePassService;
    @Autowired
    private SurveyPeriodService surveyPeriodService;

    @Autowired
    @Qualifier("apiRetry")
    private Retry retry; // Use apiRetry for API calls

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
        totalListSitesProcessTime = 0; // Resetting the counters
        totalListSitesProcessed = 0;

        long processStartTime = System.currentTimeMillis();
        try {
            int totalProcessedItems = Retry.decorateCheckedSupplier(retry, () -> { //실패했을시에 재 시작 하는 resilience4j 의 함수 적용
                long fetchStartTime = System.currentTimeMillis();
                List<ListSiteDTO> listSites = mcAtlystApiService.listSites(); //수집
                long fetchEndTime = System.currentTimeMillis();
                logger.info("--------------------------------------------------------------------------------------------");
                logger.info("Total time spent calling the listSites API: {} ms, number of items called: {}", (fetchEndTime - fetchStartTime), listSites.size());

                listSites.forEach(listSite -> mcAtlystApiService.cacheSite(listSite.getSite_id()));

                int processedItems = 0;
                if (!listSites.isEmpty()) {
                    processedItems += siteService.saveSiteLogs(listSites);
                    processedItems += surveyPeriodService.saveSurveyPeriods(listSites);
                }
                return processedItems;
            }).apply();

            totalListSitesProcessed += totalProcessedItems;
        } catch (Exception e) {
            handleApiException("Failed to fetch and cache list sites", e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            long processEndTime = System.currentTimeMillis();
            long processTime = processEndTime - processStartTime;
            totalListSitesProcessTime = processTime;
            logger.info("listSites api --> Overall success to DB load, execution time: {} ms, number of items processed: {}", processTime, totalListSitesProcessed);
            logger.info("--------------------------------------------------------------------------------------------");
        }
    }

    /**
     * 5분 간격으로 Individual Vehicles 개별 차량 호출 후 데이터를 처리한다. // TL_RIS_ROADWIDTH api 호출
     */
    @Scheduled(cron = "${scheduler.cron.IndividualVehicles}")
    public void fetchIndividualVehicles() throws Throwable {
        totalIndividualVehiclesProcessTime = 0; // Resetting the counters
        totalIndividualVehiclesProcessed = 0;

        long processStartTime = System.currentTimeMillis();
        List<Integer> processedSiteIds = new ArrayList<>();

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
            List<Integer> batchList = siteList.subList(i, end);// 배치 개수만큼 리스트에 담음

            // 각 배치 리스트의 사이트 ID를 처리
            List<IndividualVehiclesDTO> allVehicles = new ArrayList<>();
            for (Integer siteId : batchList) {
                List<IndividualVehiclesDTO> vehicles = Retry.decorateCheckedSupplier(retry, () -> fetchVehiclesForSite(siteId)).apply();
                allVehicles.addAll(vehicles);
                totalIndividualVehiclesProcessed += vehicles.size();
                processedSiteIds.add(siteId);
            }
            logger.info("--------------------------------------------------------------------------------------------");
            logger.info("Total time spent calling the IndividualVehicles api: {} ms", totalIndividualVehiclesProcessTime);

            // 배치로 차량 데이터를 저장
            if (!allVehicles.isEmpty()) {
                vehiclePassService.saveVehiclePasses(allVehicles);
            }
        }

        // 모든 작업이 완료된 후 마지막 차량 통과 시간을 파일에 저장
        VehicleUtils.LastVehiclePassTimeManager.saveLastVehiclePassTimes();
        long processEndTime = System.currentTimeMillis();

        logger.info("IndividualVehicles api --> Overall success to DB load, execution time: {} ms, number of items processed: {}", processEndTime - processStartTime, totalIndividualVehiclesProcessed);
        logger.info("--------------------------------------------------------------------------------------------");
        logger.info("--------------------------------------------------------------------------------------------");
        logger.info("Processed site IDs: {}", processedSiteIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
        logger.info("--------------------------------------------------------------------------------------------");
    }


    // 각 사이트에 대해 차량 데이터를 가져오는 메서드
    private List<IndividualVehiclesDTO> fetchVehiclesForSite(Integer siteId) {
        List<IndividualVehiclesDTO> vehicles = new ArrayList<>();
        try {
            long fetchStartTime = System.currentTimeMillis();
            vehicles = mcAtlystApiService.individualVehicles(siteId);
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
