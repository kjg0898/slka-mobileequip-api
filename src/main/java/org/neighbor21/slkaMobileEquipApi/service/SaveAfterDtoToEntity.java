package org.neighbor21.slkaMobileEquipApi.service;

import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.SurveyPeriodDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_CUREntity;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_LOGEntity;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PASSEntity;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_LOG_IdEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PASS_IdEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;
import org.neighbor21.slkaMobileEquipApi.jpaRepository.TL_MVMNEQ_CURRepository;
import org.neighbor21.slkaMobileEquipApi.jpaRepository.TL_MVMNEQ_PERIODRepository;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;


/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.dtoToEntity
 * fileName       : SaveAfterDtoToEntity.java
 * author         : kjg08
 * date           : 24. 4. 18.
 * description    : DTO를 조합하여 엔티티를 만들어 DB에 저장하는 로직을 제공하는 서비스 클래스입니다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 18.        kjg08           최초 생성
 */
@Service
public class SaveAfterDtoToEntity {

    private static final Logger logger = LoggerFactory.getLogger(SaveAfterDtoToEntity.class);

    // siteId별 마지막 통과 시간 저장
    private final Map<Integer, Timestamp> lastPassTimeMap = new HashMap<>();

    @Autowired
    private TL_MVMNEQ_CURRepository tlMvmneqCurRepository;

    @Autowired
    private TL_MVMNEQ_PERIODRepository tlMvmneqPeriodRepository;

    @Autowired
    private BatchService batchService;

    /**
     * 장소 목록을 받아와서 DB에 저장하고, 이력을 남기는 메소드입니다.
     *
     * @param locations List<ListSiteDTO> 장소 목록
     */
    @Transactional
    public void SiteLogServiceTL_MVMNEQ(List<ListSiteDTO> locations) {

        List<TL_MVMNEQ_CUREntity> curEntities = new ArrayList<>();
        List<TL_MVMNEQ_LOGEntity> logEntities = new ArrayList<>();

        locations.forEach(location -> {
            try {
                // 설치 위치 관리 테이블에서 장비아이디 값 조회 후 가져오기, 없으면 새로 엔티티 객체 생성
                TL_MVMNEQ_CUREntity tlMvmneqCurEntity = tlMvmneqCurRepository.findByEqpmntId(location.getAsset_management_id())
                        .orElse(new TL_MVMNEQ_CUREntity());

                boolean isNew = tlMvmneqCurEntity.getEqpmntId() == null;
                boolean isChanged = isEntityChanged(location, tlMvmneqCurEntity);

                if (isChanged || isNew) {
                    // 설치 위치 관리 테이블 리스트에 값 추가
                    updateCurrentEntity(location, tlMvmneqCurEntity);
                    curEntities.add(tlMvmneqCurEntity);

                    // 설치 위치 이력 테이블 리스트에 값 추가
                    TL_MVMNEQ_LOGEntity tlMvmneqLogEntity = createLogEntity(location);
                    logEntities.add(tlMvmneqLogEntity);
                }
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_CUR/LOG 처리 중 오류 발생", e);
            }
        });

        // 배치 처리하여 한번에 리스트를 삽입한다.
        insertEntities(curEntities, logEntities);
    }

    /**
     * 엔티티가 변경되었는지 여부를 판단하는 메소드입니다.
     *
     * @param location          ListSiteDTO 장소 정보
     * @param tlMvmneqCurEntity TL_MVMNEQ_CUREntity 엔티티
     * @return boolean 엔티티가 변경되었으면 true, 아니면 false
     */
    private boolean isEntityChanged(ListSiteDTO location, TL_MVMNEQ_CUREntity tlMvmneqCurEntity) {
        return !tlMvmneqCurEntity.getInstllcId().equals(location.getSite_id().toString()) ||
                !tlMvmneqCurEntity.getInstllcNm().equals(location.getName()) ||
                !tlMvmneqCurEntity.getInstllcDescr().equals(location.getDescription()) ||
                tlMvmneqCurEntity.getLatitude().compareTo(BigDecimal.valueOf(location.getLatitude())) != 0 ||
                tlMvmneqCurEntity.getLongitude().compareTo(BigDecimal.valueOf(location.getLongitude())) != 0;
    }

    /**
     * 설치 위치 관리 테이블에 값을 추가하는 메소드입니다.
     *
     * @param location          ListSiteDTO 장소 정보
     * @param tlMvmneqCurEntity TL_MVMNEQ_CUREntity 엔티티
     */
    private void updateCurrentEntity(ListSiteDTO location, TL_MVMNEQ_CUREntity tlMvmneqCurEntity) {
        tlMvmneqCurEntity.setInstllcNm(location.getName());
        tlMvmneqCurEntity.setInstllcId(location.getSite_id().toString());
        tlMvmneqCurEntity.setInstllcDescr(location.getDescription());
        tlMvmneqCurEntity.setEqpmntId(location.getAsset_management_id());
        tlMvmneqCurEntity.setLatitude(BigDecimal.valueOf(location.getLatitude()));
        tlMvmneqCurEntity.setLongitude(BigDecimal.valueOf(location.getLongitude()));
        logger.info("설치 위치 관리 테이블 업데이트 성공 (장비 ID: {}): {}", location.getAsset_management_id(), tlMvmneqCurEntity);
    }

    /**
     * 설치 위치 이력 테이블에 값을 추가하는 메소드입니다.
     *
     * @param location ListSiteDTO 장소 정보
     * @return TL_MVMNEQ_LOGEntity 설치 위치 이력 엔티티
     */
    private TL_MVMNEQ_LOGEntity createLogEntity(ListSiteDTO location) {
        TL_MVMNEQ_LOGEntity tlMvmneqLogEntity = new TL_MVMNEQ_LOGEntity();
        TL_MVMNEQ_LOG_IdEntity logIdEntity = new TL_MVMNEQ_LOG_IdEntity();

        logIdEntity.setCollectionDatetime(new Timestamp(System.currentTimeMillis()));
        logIdEntity.setInstllcId(location.getSite_id().toString());
        tlMvmneqLogEntity.setId(logIdEntity);

        tlMvmneqLogEntity.setInstllcNm(location.getName());
        tlMvmneqLogEntity.setInstllcDescr(location.getDescription());
        tlMvmneqLogEntity.setEqpmntId(location.getAsset_management_id());
        tlMvmneqLogEntity.setLatitude(BigDecimal.valueOf(location.getLatitude()));
        tlMvmneqLogEntity.setLongitude(BigDecimal.valueOf(location.getLongitude()));

        logger.info("설치 위치 이력 테이블에 로그 기록 성공 (장비 ID: {}): {}", location.getAsset_management_id(), tlMvmneqLogEntity);
        return tlMvmneqLogEntity;
    }

    /**
     * 배치 처리하여 엔티티 리스트를 한번에 삽입하는 메소드입니다.
     *
     * @param curEntities List<TL_MVMNEQ_CUREntity> 현재 엔티티 리스트
     * @param logEntities List<TL_MVMNEQ_LOGEntity> 이력 엔티티 리스트
     */
    private void insertEntities(List<TL_MVMNEQ_CUREntity> curEntities, List<TL_MVMNEQ_LOGEntity> logEntities) {
        try {
            batchService.batchInsertWithRetry(curEntities);
            batchService.batchInsertWithRetry(logEntities);
            logger.info("TL_MVMNEQ_CUR 및 TL_MVMNEQ_LOG 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_CUR/LOG 배치 삽입 실패", e);
        }
    }

    /**
     * 이동형 장비 통과 차량 정보를 엔티티로 변환 후 테이블에 적재하는 메소드입니다.
     * 5분마다 실행되며, 이전 통과 차량 시간 정보를 파일에 저장한 후에 다시 그 시간을 읽어 그 다음 시간부터 조회한 정보를 DB에 저장합니다.
     *
     * @param vehicles List<IndividualVehiclesDTO> 개별 차량 정보 리스트
     */
    @Transactional
    public void insertTL_MVMNEQ_PASS(List<IndividualVehiclesDTO> vehicles) {
        List<TL_MVMNEQ_PASSEntity> passEntities = new ArrayList<>();
        vehicles.forEach(vehicle -> {
            try {
                Integer siteId = vehicle.getSiteId();
                // 각 위치별 지난 통행 시간 또는 현재 시간
                Timestamp lastPassTime = lastPassTimeMap.getOrDefault(vehicle.getSiteId(), new Timestamp(System.currentTimeMillis()));
                // 각 위치별 현재 통행 시간
                Timestamp currentTimestamp = new Timestamp(vehicle.getTimestamp().getTime());

                TL_MVMNEQ_PASSEntity tlMvmneqPassEntity = new TL_MVMNEQ_PASSEntity();
                TL_MVMNEQ_PASS_IdEntity passIdEntity = new TL_MVMNEQ_PASS_IdEntity();

                passIdEntity.setPassTime(currentTimestamp);
                passIdEntity.setVehicleDirection(vehicle.getHeading());
                passIdEntity.setPassLane(vehicle.getLaneIndex());
                passIdEntity.setInstllcId(siteId.toString());

                tlMvmneqPassEntity.setId(passIdEntity);
                tlMvmneqPassEntity.setVehicleSpeed(vehicle.getVelocity());
                tlMvmneqPassEntity.setVehicleLength(vehicle.getLength());
                tlMvmneqPassEntity.setVehicleHeadway(VehicleUtils.calculateHeadway(currentTimestamp, lastPassTime));
                tlMvmneqPassEntity.setVehicleClass(vehicle.getVehicleClass());

                // 현재 통과 시간을 마지막 통과 시간으로 업데이트
                lastPassTimeMap.put(siteId, currentTimestamp);

                passEntities.add(tlMvmneqPassEntity);
                logger.info("TL_MVMNEQ_PASS 테이블에 삽입 성공 (장소 ID: {}): {}", vehicle.getSiteId(), tlMvmneqPassEntity);
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_PASS 처리 중 오류 발생", e);
            }
        });

        try {
            batchService.batchInsertWithRetry(passEntities);
            logger.info("TL_MVMNEQ_PASS 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PASS 배치 삽입 실패", e);
        }
    }

    /**
     * 이동형 장비 조사 기간 정보를 엔티티로 변환 후 테이블에 적재하는 메소드입니다.
     * 일시적으로 데이터 수집하는 장소의 경우 각 설문조사 기간마다 survey_periods 배열에 별도로 추가합니다.
     * 영구적으로 수집하는 장소는 survey_periods 배열에 해당하는 요소에 새 데이터가 들어올 때마다 설문조사 기간의 종료 시간을 업데이트합니다.
     *
     * @param periods List<ListSiteDTO> 조사 기간 정보 리스트
     */
    @Transactional
    public void insertTL_MVMNEQ_PERIOD(List<ListSiteDTO> periods) {
        List<TL_MVMNEQ_PERIODEntity> periodEntities = new ArrayList<>();
        periods.forEach(period -> {
            try {
                // 순번을 정하기 위해 List Sites. survey_periods의 start_time 기준으로 정렬
                List<SurveyPeriodDTO> sortedPeriods = period.getSurvey_periods().stream()
                        .sorted(Comparator.comparing(SurveyPeriodDTO::getStart_time))
                        .toList();

                String instllcId = period.getSite_id().toString();
                // 이 지역의 및 날짜/시간에 대한 현재 최대 순번을 가져옵니다.
                Integer currentMaxSequence = tlMvmneqPeriodRepository.findMaxSequenceNoByInstllcId(new Timestamp(System.currentTimeMillis()), instllcId);

                for (int i = 0; i < sortedPeriods.size(); i++) {
                    SurveyPeriodDTO periodDTO = sortedPeriods.get(i);
                    TL_MVMNEQ_PERIODEntity periodEntity = new TL_MVMNEQ_PERIODEntity();
                    TL_MVMNEQ_PERIOD_IdEntity periodIdEntity = new TL_MVMNEQ_PERIOD_IdEntity();

                    periodIdEntity.setCollectionDatetime(new Timestamp(System.currentTimeMillis()));
                    periodIdEntity.setSequenceNo(currentMaxSequence + i + 1);  // 발견된 순번의 최대값을 기준으로 +1
                    periodIdEntity.setInstllcId(instllcId);

                    periodEntity.setId(periodIdEntity);
                    periodEntity.setStartTime(Timestamp.valueOf(periodDTO.getStart_time()));
                    periodEntity.setEndTime(Timestamp.valueOf(periodDTO.getEnd_time()));

                    periodEntities.add(periodEntity);
                    logger.info("TL_MVMNEQ_PERIOD 테이블에 삽입 성공 (설치 위치 ID: {}): {}", instllcId, periodEntity);
                }
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_PERIOD 처리 중 오류 발생", e);
            }
        });

        try {
            batchService.batchInsertWithRetry(periodEntities);
            logger.info("TL_MVMNEQ_PERIOD 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PERIOD 배치 삽입 실패", e);
        }
    }
}