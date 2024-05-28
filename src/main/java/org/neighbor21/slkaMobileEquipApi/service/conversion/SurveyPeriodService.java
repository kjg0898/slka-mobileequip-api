package org.neighbor21.slkaMobileEquipApi.service.conversion;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.SurveyPeriodDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;
import org.neighbor21.slkaMobileEquipApi.jpaRepository.TL_MVMNEQ_PERIODRepository;
import org.neighbor21.slkaMobileEquipApi.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.conversion
 * fileName       : SurveyPeriodService.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    : 조사 기간 데이터를 처리하고 저장하는 서비스 클래스.
 * 이 클래스는 조사 기간 데이터를 받아와서 데이터베이스에 저장하는 작업을 수행한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
 */

@Service
public class SurveyPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyPeriodService.class);

    @Autowired
    private TL_MVMNEQ_PERIODRepository tlMvmneqPeriodRepository;

    @Autowired
    private BatchService batchService;

    @PersistenceContext
    private EntityManager entityManager;


    /**
     * 조사 기간 정보를 받아와서 데이터베이스에 저장하는 메소드.
     *
     * @param periods List<ListSiteDTO> 조사 기간 정보 리스트
     */
    @Transactional
    public void saveSurveyPeriods(List<ListSiteDTO> periods) {
        List<TL_MVMNEQ_PERIODEntity> periodEntities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        periods.forEach(period -> {
            try {
                // 조사 기간을 시작 시간 기준으로 정렬 (스트림 사용 sorted)
                List<SurveyPeriodDTO> sortedPeriods = period.getSurvey_periods().stream()
                        .sorted(Comparator.comparing(SurveyPeriodDTO::getStart_time))
                        .toList();

                String instllcId = period.getSite_id().toString();
                // 현재 설치 위치 ID에 대한 최대 순번을 가져옴
                Integer currentMaxSequence = tlMvmneqPeriodRepository.findMaxSequenceNoByInstllcId(instllcId);

                // 각 조사 기간을 엔티티로 변환하여 리스트에 추가
                for (int i = 0; i < sortedPeriods.size(); i++) {
                    SurveyPeriodDTO periodDTO = sortedPeriods.get(i);
                    TL_MVMNEQ_PERIODEntity periodEntity = new TL_MVMNEQ_PERIODEntity();
                    TL_MVMNEQ_PERIOD_IdEntity periodIdEntity = new TL_MVMNEQ_PERIOD_IdEntity();

                    periodIdEntity.setCollectionDatetime(Timestamp.valueOf(LocalDateTime.now().format(formatter)));
                    periodIdEntity.setSequenceNo(currentMaxSequence + i + 1);
                    periodIdEntity.setInstllcId(instllcId);

                    periodEntity.setId(periodIdEntity);
                    periodEntity.setStartTime(Timestamp.valueOf(periodDTO.getStart_time().replace("T", " ")));
                    periodEntity.setEndTime(Timestamp.valueOf(periodDTO.getEnd_time().replace("T", " ")));

                    periodEntities.add(periodEntity);
                }
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_PERIOD 처리 중 오류 발생", e);
            }
        });

        // 엔티티 리스트를 배치로 삽입
        long dbStartTime = System.currentTimeMillis();
        try {
            batchService.batchInsertWithRetry(periodEntities, this::insertPeriodEntity);
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PERIOD 배치 삽입 실패", e);
        }
        long dbEndTime = System.currentTimeMillis();
        logger.info("TL_MVMNEQ_PERIOD 배치 삽입 작업에 걸린 시간: {} ms", (dbEndTime - dbStartTime));
    }

    /**
     * 조사 기간 엔티티를 삽입하는 메소드.
     *
     * @param entityManager EntityManager
     * @param entity        TL_MVMNEQ_PERIODEntity
     */
    private void insertPeriodEntity(EntityManager entityManager, TL_MVMNEQ_PERIODEntity entity) {
        String query = "INSERT INTO srlk.tl_mvmneq_period (clct_dt, instllc_id, sqno, start_dt, end_dt) " +
                "VALUES (:clctDt, :instllcId, :sqno, :startDt, :endDt) " +
                "ON CONFLICT (clct_dt, instllc_id, sqno) DO NOTHING";

        entityManager.createNativeQuery(query)
                .setParameter("clctDt", entity.getId().getCollectionDatetime())
                .setParameter("instllcId", entity.getId().getInstllcId())
                .setParameter("sqno", entity.getId().getSequenceNo())
                .setParameter("startDt", entity.getStartTime())
                .setParameter("endDt", entity.getEndTime())
                .executeUpdate();
    }
}