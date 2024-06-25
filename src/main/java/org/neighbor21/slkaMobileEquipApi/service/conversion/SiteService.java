package org.neighbor21.slkaMobileEquipApi.service.conversion;

import io.github.resilience4j.retry.Retry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_CUREntity;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_LOGEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_CUR_IdEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_LOG_IdEntity;
import org.neighbor21.slkaMobileEquipApi.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 장소 데이터를 처리하고 저장하는 서비스 클래스.
 */
@Service
public class SiteService {

    private static final Logger logger = LoggerFactory.getLogger(SiteService.class);

    @Autowired
    private BatchService batchService;

    @Autowired
    @Qualifier("dbRetry")
    private Retry retry; // Use dbRetry for database operations Retry 객체 주입

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 장소 목록을 받아와서 현재 설치 위치와 설치 이력을 관리하는 메소드.
     *
     * @param locations List<ListSiteDTO> 장소 목록
     * @return int 처리된 항목 수
     */
    @Transactional
    public int saveSiteLogs(List<ListSiteDTO> locations) {
        List<TL_MVMNEQ_CUREntity> curEntities = new ArrayList<>();
        List<TL_MVMNEQ_LOGEntity> logEntities = new ArrayList<>();
        // 각 장소에 대해 처리
        for (ListSiteDTO location : locations) {
            try {
                TL_MVMNEQ_CUREntity curEntity = createCurrentEntity(location);
                curEntities.add(curEntity);

                TL_MVMNEQ_LOGEntity logEntity = createLogEntity(location);
                logEntities.add(logEntity);
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_CUR/LOG 처리 중 오류 발생", e);
            }
        }

        // 엔티티 리스트를 배치로 삽입
        try {
            long dbStartTime = System.currentTimeMillis();
            Retry.decorateRunnable(retry, () -> batchService.batchInsertWithRetry(curEntities, entityManager::persist)).run();
            long dbEndTime = System.currentTimeMillis();
            logger.info("TL_MVMNEQ_CUR Batch insertion successful, total time taken: {} ms, number of items inserted: {}", (dbEndTime - dbStartTime), curEntities.size());
            // 하이버네이트의 1차 캐시를 플러쉬하고 클리어하여 DB와의 불일치 방지
            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
            entityManager.clear(); // 영속성 컨텍스트를 비움
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_CUR 배치 삽입 실패", e);
            // 예외 발생 시, 추가적인 예외 처리를 수행할 수 있습니다. 예: 알림 발송, 재시도 로직 등
        }

        try {
            long dbStartTime = System.currentTimeMillis();
            Retry.decorateRunnable(retry, () -> batchService.batchInsertWithRetry(logEntities, entityManager::persist)).run();
            long dbEndTime = System.currentTimeMillis();
            logger.info("TL_MVMNEQ_LOG Batch insertion successful, total time taken: {} ms, number of items inserted: {}", (dbEndTime - dbStartTime), logEntities.size());
            // 하이버네이트의 1차 캐시를 플러쉬하고 클리어하여 DB와의 불일치 방지
            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
            entityManager.clear(); // 영속성 컨텍스트를 비움
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_LOG 배치 삽입 실패", e);
        }
        return curEntities.size() + logEntities.size();
    }

    /**
     * 설치 위치 관리 테이블에 값을 추가하는 메소드.
     *
     * @param location ListSiteDTO 장소 정보
     * @return TL_MVMNEQ_CUREntity 생성된 엔티티
     */
    private TL_MVMNEQ_CUREntity createCurrentEntity(ListSiteDTO location) {
        TL_MVMNEQ_CUREntity tlMvmneqCurEntity = new TL_MVMNEQ_CUREntity();
        TL_MVMNEQ_CUR_IdEntity newId = new TL_MVMNEQ_CUR_IdEntity(location.getSite_id().toString());

        tlMvmneqCurEntity.setInstllcId(newId);
        tlMvmneqCurEntity.setInstllcNm(location.getName());
        tlMvmneqCurEntity.setInstllcDescr(location.getDescription());
        tlMvmneqCurEntity.setLatitude(BigDecimal.valueOf(location.getLatitude()));
        tlMvmneqCurEntity.setLongitude(BigDecimal.valueOf(location.getLongitude()));
        tlMvmneqCurEntity.setEqpmntId(location.getAsset_management_id());
        tlMvmneqCurEntity.setCollectionDatetime(new Timestamp(System.currentTimeMillis()));

        return tlMvmneqCurEntity;
    }

    /**
     * 설치 위치 이력 테이블에 값을 추가하는 메소드.
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

        return tlMvmneqLogEntity;
    }
}
