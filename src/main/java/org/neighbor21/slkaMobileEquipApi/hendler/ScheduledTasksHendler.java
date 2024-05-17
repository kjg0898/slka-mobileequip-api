package org.neighbor21.slkaMobileEquipApi.hendler;

import jakarta.annotation.PostConstruct;
import kong.unirest.UnirestException;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.service.MCATLYSTApiService;
import org.neighbor21.slkaMobileEquipApi.service.SaveAfterDtoToEntity;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


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
public class ScheduledTasksHendler {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksHendler.class);

    // 마지막 차량 통과 시간 파일 저장 매니저
    private final VehicleUtils.LastVehiclePassTimeManager lastVehiclePassTimeManager = new VehicleUtils.LastVehiclePassTimeManager();

    @Autowired
    private MCATLYSTApiService mcAtlystApiService;

    @Autowired
    private SaveAfterDtoToEntity saveAfterDtoToEntity;

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
        try {
            // 리스트 선언
            List<ListSiteDTO> listSites = mcAtlystApiService.listSites();

            // site_id 목록 별로 ListSiteDTO 객체 목록을 반복하여 각 객체를 cacheSite 메서드에 전달
            listSites.forEach(listSite -> mcAtlystApiService.cacheSite(listSite.getSite_id()));

            if (!listSites.isEmpty()) {
                // TL_MVMNEQ_CUR 테이블에 이동형 장비 설치위치 관리, TL_MVMNEQ_LOG 테이블에 이동형 장비 설치위치 이력 업데이트
                saveAfterDtoToEntity.SiteLogServiceTL_MVMNEQ(listSites);

                // TL_MVMNEQ_PERIOD 테이블에 이동형 장비 조사기간 정보 데이터 insert
                saveAfterDtoToEntity.insertTL_MVMNEQ_PERIOD(listSites);
            }
        } catch (UnirestException e) {
            handleApiException("Failed to fetch and cache list sites", e);
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

        // siteCache에 저장된 site_id 별로
        mcAtlystApiService.getSiteCache().forEach(siteId -> {
            try {
                logger.info("Processing individual vehicles for siteId: {}", siteId);

                // 저장된 site_id 별로 api 호출
                List<IndividualVehiclesDTO> vehicles = mcAtlystApiService.individualVehicles(siteId);

                if (!vehicles.isEmpty()) {
                    // TL_MVMNEQ_PASS 테이블에 이동형 장비 통과 차량 정보 데이터 insert
                    saveAfterDtoToEntity.insertTL_MVMNEQ_PASS(vehicles);

                    // 현재 차량 지나간 시간을 최신화
                    lastVehiclePassTimeManager.saveLastVehiclePassTimes();
                }
            } catch (UnirestException e) {
                handleApiException(String.format("Failed to fetch individual vehicles for siteId: %s", siteId), e);
            }
        });
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
