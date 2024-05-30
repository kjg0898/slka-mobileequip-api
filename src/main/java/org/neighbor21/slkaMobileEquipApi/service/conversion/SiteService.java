package org.neighbor21.slkaMobileEquipApi.service.conversion;

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

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 장소 목록을 받아와서 현재 설치 위치와 설치 이력을 관리하는 메소드.
     *
     * @param locations List<ListSiteDTO> 장소 목록
     */
    @Transactional
    public void saveSiteLogs(List<ListSiteDTO> locations) {
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
            batchService.batchInsertWithRetry(curEntities, entityManager::persist);
            long dbEndTime = System.currentTimeMillis();
            logger.info("TL_MVMNEQ_LOG 배치 삽입 작업에 걸린 총 시간: {} ms", (dbEndTime - dbStartTime));
            //하이버네이트에는 일시적으로 db 메모리를 1차캐시에 저장하는데, 네이티브 쿼리를 사용하면 그 캐쉬를 지나지 않고 바로 작용하기 때문에 네이티브쿼리
            //작업이 끝난 후에 플러쉬 클리어를 해주는 것이 좋다. 안그러면 디비 메모리와 캐시의 불일치가 일어날수 있기때문이다.
            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
            entityManager.clear(); // 영속성 컨텍스트를 비움

        } catch (Exception e) {
            logger.error("TL_MVMNEQ_CUR 배치 삽입 실패", e);
        }
        try {
            long dbStartTime = System.currentTimeMillis();
            batchService.batchInsertWithRetry(logEntities, entityManager::persist);
            long dbEndTime = System.currentTimeMillis();
            logger.info("TL_MVMNEQ_LOG 배치 삽입 작업에 걸린 총 시간: {} ms", (dbEndTime - dbStartTime));
            //하이버네이트에는 일시적으로 db 메모리를 1차캐시에 저장하는데, 네이티브 쿼리를 사용하면 그 캐쉬를 지나지 않고 바로 작용하기 때문에 네이티브쿼리
            //작업이 끝난 후에 플러쉬 클리어를 해주는 것이 좋다. 안그러면 디비 메모리와 캐시의 불일치가 일어날수 있기때문이다.
            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
            entityManager.clear(); // 영속성 컨텍스트를 비움

        } catch (Exception e) {
            logger.error("TL_MVMNEQ_LOG 배치 삽입 실패", e);
        }
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
