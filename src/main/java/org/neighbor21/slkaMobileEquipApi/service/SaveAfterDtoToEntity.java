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
 * description    : dto 를 조합하여 엔티티를 만들어 db 에 save 하는 로직
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

    /*// 시스템 을 새롭게 시작하거나 재시작 했을때 프로그램 메모리 안에 이동형장비 설치위치 정보가 없기 때문에  업데이트 하고 이력을 삽입할때 중복값 비교를 하여 새로운 값에 대해서만 작동하지 않고
    // 중복값에 대해 처리될 것이기 때문에 초기 데이터(가장 최근에 적재된 이동형 장비 설치위치 테이블) 를 로드 하여 비교할수 있도록 엔티티에 넣어둔다.
    //초기 디폴트 값을 db 에 넣어 굳이 비교할 필요가 없어서 주석처리함. 그리고 하드코딩한 임의의 데이터를 db 에 넣는것은 좋지 않으므로 test 용 외에는 사용하지 않을것임.
    @PostConstruct
    public void initSiteData() {
        List<TL_MVMNEQ_CUR> currentData = tlMvmneqCurRepository.findAll();
        if (currentData.isEmpty()) {
            logger.info("No Default site data i_CUR data found at startup. Initializing default data.");
            initializeDefaultSiteData();  // 이 부분은 데이터가 전혀 없을 때를 대비해 기본 데이터를 설정하는 로직입니다.
        } else {
            logger.info("Existing TL_MVMNEQ_CUR data loaded successfully.");
        }
    }

    private void initializeDefaultSiteData() {
        // 기본 데이터 생성 예제
        TL_MVMNEQ_CUR defaultSite = new TL_MVMNEQ_CUR();
        defaultSite.setInstllcId("default");
        defaultSite.setInstllcNm("Default Location");
        defaultSite.setInstllcDescr("This is an automatically generated default location.");
        defaultSite.setEqpmntId("0000");
        defaultSite.setLatitude(BigDecimal.valueOf(0.0));
        defaultSite.setLongitude(BigDecimal.valueOf(0.0));
        tlMvmneqCurRepository.save(defaultSite);
        logger.info("Default site data initialized.");
    }*/


    //이전 데이터와 변동값있거나 새로운 장비 아이디 값일때 이동형 장비 설치위치를 해당 db 에 매칭되는 엔티티에 저장하고 이동형 장비 설치 테이블에 적재 한 후 이동형 장비 설치 위치 이력 테이블에 저장한다
    //이 데이터 , 즉 list.site 는 개별 차량과 같이 계속 생성되는것이 아니라 정해진 장소목록에 대해서만 나오므로, 또 한 api 호출 단계에서 매개변수로 보낼수 있는 시간값이 없기 때문에 각 site_id 별로
    //dto 에서 엔티티로 저장하기 전에 값을 비교 하는 방식으로 리소스 소모를 줄였다.
    @Transactional
    public void SiteLogServiceTL_MVMNEQ(List<ListSiteDTO> locations) {

        List<TL_MVMNEQ_CUREntity> curEntities = new ArrayList<>();
        List<TL_MVMNEQ_LOGEntity> logEntities = new ArrayList<>();

        locations.forEach(location -> {
            try {
                //설치 위치 관리 테이블 에서 장비아이디 값 조회 후 가져오기 없으면 새로 엔티티 객체 생성
                //설치위치 관리 테이블 추가
                TL_MVMNEQ_CUREntity tlMvmneqCurEntity = tlMvmneqCurRepository.findByEqpmntId(location.getAsset_management_id())
                        .orElse(new TL_MVMNEQ_CUREntity());

                boolean isNew = tlMvmneqCurEntity.getEqpmntId() == null;
                boolean isChanged = !isNew && (
                        !tlMvmneqCurEntity.getInstllcId().equals(location.getSite_id().toString()) ||
                                !tlMvmneqCurEntity.getInstllcNm().equals(location.getName()) ||
                                !tlMvmneqCurEntity.getInstllcDescr().equals(location.getDescription()) ||
                                tlMvmneqCurEntity.getLatitude().compareTo(BigDecimal.valueOf(location.getLatitude())) != 0 ||
                                tlMvmneqCurEntity.getLongitude().compareTo(BigDecimal.valueOf(location.getLongitude())) != 0
                );
                if (isChanged || isNew) {
                    //설치위치 아이디
                    tlMvmneqCurEntity.setInstllcId(location.getSite_id().toString());
                    //설치위치 명
                    tlMvmneqCurEntity.setInstllcNm(location.getName());
                    //설치위치 설명
                    tlMvmneqCurEntity.setInstllcDescr(location.getDescription());
                    //장비 아이디
                    tlMvmneqCurEntity.setEqpmntId(location.getAsset_management_id());
                    //위도
                    tlMvmneqCurEntity.setLatitude(BigDecimal.valueOf(location.getLatitude()));
                    //경도
                    tlMvmneqCurEntity.setLongitude(BigDecimal.valueOf(location.getLongitude()));

                    curEntities.add(tlMvmneqCurEntity); //엔티티를 TL_MVMNEQ_CUREntity 리스트에 저장
                    logger.info("Successfully updated TL_MVMNEQ_CUR for Equipment ID {}: {}", location.getAsset_management_id(), tlMvmneqCurEntity);


                    //설치위치 이력 테이블 추가
                    TL_MVMNEQ_LOGEntity tlMvmneqLogEntity = new TL_MVMNEQ_LOGEntity();
                    TL_MVMNEQ_LOG_IdEntity logIdEntity = new TL_MVMNEQ_LOG_IdEntity();

                    //수집일시(현재시간), 설치 위치 아이디(복합키)
                    logIdEntity.setCollectionDatetime(new Timestamp(System.currentTimeMillis()));
                    logIdEntity.setInstllcId(location.getSite_id().toString());
                    tlMvmneqLogEntity.setId(logIdEntity);

                    //설치 위치 명
                    tlMvmneqLogEntity.setInstllcNm(location.getName());
                    //설치 위치 설명
                    tlMvmneqLogEntity.setInstllcDescr(location.getDescription());
                    //장비 아이디
                    tlMvmneqLogEntity.setEqpmntId(location.getAsset_management_id());
                    //위도
                    tlMvmneqLogEntity.setLatitude(BigDecimal.valueOf(location.getLatitude()));
                    //경도
                    tlMvmneqLogEntity.setLongitude(BigDecimal.valueOf(location.getLongitude()));

                    logEntities.add(tlMvmneqLogEntity); //엔티티를 TL_MVMNEQ_LOGEntity 리스트에 저장
                    logger.info("Successfully logged in TL_MVMNEQ_LOG for Equipment ID {}: {}", location.getAsset_management_id(), tlMvmneqLogEntity);
                }
            } catch (Exception e) {
                logger.error("Error during processing TL_MVMNEQ_CUR/LOG", e);
            }
        });
        // 배치 처리하여 한번에 리스트를 삽입한다.
        try {
            batchService.batchInsertWithRetry(curEntities);
            batchService.batchInsertWithRetry(logEntities);
            logger.info("Batch insert for TL_MVMNEQ_CUR and TL_MVMNEQ_LOG completed.");
        } catch (Exception e) {
            logger.error("Batch insert failed for TL_MVMNEQ_CUR/LOG", e);
        }
    }

    //이동형 장비 통과 차량 정보를 엔티티로 변환 후 테이블에 적재한다. 5분마다 실행되며, 이전 통과차량 시간정보를 파일에 저장한 후에 다시 그 시간을 읽어 그 다음 시간부터 조회한 정보를 db 에 저장한다.
    //api 에서 호출 할 당시부터 매개변수 값으로 조회 시간을 보내면서 중복값 호출을 방지 하였으므로 이곳에서는 전부 저장해도 무관하다.
    @Transactional
    public void insertTL_MVMNEQ_PASS(List<IndividualVehiclesDTO> vehicles) {
        List<TL_MVMNEQ_PASSEntity> passEntities = new ArrayList<>();
        // todo 파일로 최근시간 가져올지, db 로 져올지는 생각 해봐야 함 , 일단은 파일로.
        vehicles.forEach(vehicle -> {
            try {
                Integer siteId = vehicle.getSiteId();
                // 각 위치별 지난 통행 시간 또는 현재 시간
                Timestamp lastPassTime = lastPassTimeMap.getOrDefault(vehicle.getSiteId(), new Timestamp(System.currentTimeMillis()));

                //각 위치별 현재 통행 시간
                Timestamp currentTimestamp = new Timestamp(vehicle.getTimestamp().getTime());

                TL_MVMNEQ_PASSEntity tlMvmneqPassEntity = new TL_MVMNEQ_PASSEntity();
                TL_MVMNEQ_PASS_IdEntity passIdEntity = new TL_MVMNEQ_PASS_IdEntity();

                //통행 일시
                passIdEntity.setPassTime(currentTimestamp);
                //차량 방향
                passIdEntity.setVehicleDirection(vehicle.getHeading());
                //통행 차로
                passIdEntity.setPassLane(vehicle.getLaneIndex());
                //설치 위치 아이디
                passIdEntity.setInstllcId(siteId.toString());// IndividualVehicle 를 조회할때 사용한 해당 site ID

                tlMvmneqPassEntity.setId(passIdEntity);

                //차량 속도
                tlMvmneqPassEntity.setVehicleSpeed(vehicle.getVelocity());
                //차량 길이
                tlMvmneqPassEntity.setVehicleLength(vehicle.getLength());
                //차량 간격 초 계산 및 설정
                tlMvmneqPassEntity.setVehicleHeadway(VehicleUtils.calculateHeadway(currentTimestamp, lastPassTime));
                //차량
                tlMvmneqPassEntity.setVehicleClass(vehicle.getVehicleClass());

                // 현재 통과 시간을 마지막 통과 시간으로 업데이트
                lastPassTimeMap.put(siteId, currentTimestamp);

                passEntities.add(tlMvmneqPassEntity); //엔티티를 TL_MVMNEQ_PASSEntity 리스트에 저장
                logger.info("Successfully inserted TL_MVMNEQ_PASS for Vehicle at site {}: {}", vehicle.getSiteId(), tlMvmneqPassEntity);
            } catch (Exception e) {
                logger.error("Error during processing TL_MVMNEQ_PASS", e);
            }
        });

        try {
            batchService.batchInsertWithRetry(passEntities);
            logger.info("Batch insert for TL_MVMNEQ_PASS completed.");
        } catch (Exception e) {
            logger.error("Batch insert failed for TL_MVMNEQ_PASS", e);
        }
    }


    // todo 만약 여기서 넣어야 하는 테이블이 조사시작한시간과 종료시간에 대해서 각 장비별로 필요하다면 장비 아이디 까지 넣는것이 필요하고 그것이 아니라 그냥 그 설치 위치의 조사 시작시간과 종료시간만이 필요한 것이라면 그대로 가는것이 맞음
    //이동형장비 조사기간 정보 TL_MVMNEQ_PERIOD
    //일시적으로 데이터 수집하는 장소의 경우 각 설문조사 기간마다 survey_periods 배열에 별도로 추가
    //영구적으로 수집하는 장소는 survey_periods 배열에 해당하는 요소에 새 데이터가 들어올 때마다 설문조사 기간의 종료 시간 업데이트함
    //순번에 들어가는 값은 starttime 을 기준으로 각각의 site_id 에 맞추어서 개별적으로 순번이 추가된다.  메모리 변수에 넣어서 계산 하기에는 너무 텀이 길수도 있으므로 디비를 셀렉해서 가져온다
    @Transactional
    public void insertTL_MVMNEQ_PERIOD(List<ListSiteDTO> periods) {
        List<TL_MVMNEQ_PERIODEntity> periodEntities = new ArrayList<>();
        periods.forEach(period -> {

            try {
                //순번을 정하기 위해 List Sites. survey_periods 의 start_time 기준으로 정렬
                List<SurveyPeriodDTO> sortedPeriods = period.getSurvey_periods().stream()
                        .sorted(Comparator.comparing(SurveyPeriodDTO::getStart_time))
                        .toList();

                String instllcId = period.getSite_id().toString();
                //이 지역의 및 날짜/시간에 대한 현재 최대 순번 을 가져옵니다.
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
                    logger.info("Successfully inserted TL_MVMNEQ_PERIOD for Installation ID {}: {}", instllcId, periodEntity);
                }
            } catch (Exception e) {
                logger.error("Error during processing TL_MVMNEQ_PERIOD", e);
            }
        });

        try {
            batchService.batchInsertWithRetry(periodEntities);
            logger.info("Batch insert for TL_MVMNEQ_PERIOD completed.");
        } catch (Exception e) {
            logger.error("Batch insert failed for TL_MVMNEQ_PERIOD", e);
        }
    }
}