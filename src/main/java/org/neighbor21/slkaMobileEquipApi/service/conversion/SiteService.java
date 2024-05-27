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
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.conversion
 * fileName       : SiteService.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    : 장소 데이터를 처리하고 저장하는 서비스 클래스.
 * 이 클래스는 장소 데이터를 받아와서 현재 설치 위치와 설치 이력을 관리한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
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
        long dbStartTime = System.currentTimeMillis();
        try {
            long startTime = System.currentTimeMillis();
            batchService.batchInsertWithRetry(curEntities, this::insertCurrentEntity);
            long endTime = System.currentTimeMillis();
            logger.info("batchInsertWithRetry 메서드에서 TL_MVMNEQ_CUR_IdEntity 배치 삽입에 걸린 시간: {} ms", (endTime - startTime));
            //logger.info("TL_MVMNEQ_CUR 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_CUR 배치 삽입 실패", e);
        }
        try {
            long startTime = System.currentTimeMillis();
            batchService.batchInsertWithRetry(logEntities, this::insertLogEntity);
            long endTime = System.currentTimeMillis();
            logger.info("batchInsertWithRetry 메서드에서 TL_MVMNEQ_LOGRepository 배치 삽입에 걸린 시간: {} ms", (endTime - startTime));
            //logger.info("TL_MVMNEQ_LOG 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_LOG 배치 삽입 실패", e);
        }
        long dbEndTime = System.currentTimeMillis();
        logger.info("saveSiteLogs 메서드에서 전체 데이터베이스 삽입 작업에 걸린 시간: {} ms", (dbEndTime - dbStartTime));
        //logger.info("TL_MVMNEQ_LOG 및 TL_MVMNEQ_CUR 배치 삽입 완료.");
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
        //logger.info("설치 위치 관리 엔티티 생성 (장비 ID: {}): {}", location.getAsset_management_id(), tlMvmneqCurEntity);

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

        //logger.info("설치 위치 이력 엔티티 생성 (장비 ID: {}): {}", location.getAsset_management_id(), tlMvmneqLogEntity);
        return tlMvmneqLogEntity;
    }

    // 키값 중복 없이 삽입하는 네이티브 쿼리

    /**
     * 설치 위치 관리 엔티티를 삽입하는 메소드.
     *
     * @param entityManager EntityManager
     * @param entity        TL_MVMNEQ_CUREntity
     */
    private void insertCurrentEntity(EntityManager entityManager, TL_MVMNEQ_CUREntity entity) {
        String query = "INSERT INTO srlk.tl_mvmneq_cur (eqpmnt_id, instllc_descr, instllc_nm, latitude, longitude, instllc_id) " +
                "VALUES (:eqpmntId, :instllcDescr, :instllcNm, :latitude, :longitude, :instllcId) " +
                "ON CONFLICT (instllc_id) DO UPDATE SET " +
                "eqpmnt_id = EXCLUDED.eqpmnt_id, instllc_descr = EXCLUDED.instllc_descr, instllc_nm = EXCLUDED.instllc_nm, " +
                "latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude";

        entityManager.createNativeQuery(query)
                .setParameter("eqpmntId", entity.getEqpmntId())
                .setParameter("instllcDescr", entity.getInstllcDescr())
                .setParameter("instllcNm", entity.getInstllcNm())
                .setParameter("latitude", entity.getLatitude())
                .setParameter("longitude", entity.getLongitude())
                .setParameter("instllcId", entity.getInstllcId().getInstllcId())
                .executeUpdate();
    }

    /**
     * 설치 위치 이력 엔티티를 삽입하는 메소드.
     *
     * @param entityManager EntityManager
     * @param entity        TL_MVMNEQ_LOGEntity
     */
    private void insertLogEntity(EntityManager entityManager, TL_MVMNEQ_LOGEntity entity) {
        String query = "INSERT INTO srlk.tl_mvmneq_log (clct_dt, instllc_id, instllc_nm, instllc_descr, eqpmnt_id, latitude, longitude) " +
                "VALUES (:clctDt, :instllcId, :instllcNm, :instllcDescr, :eqpmntId, :latitude, :longitude) " +
                "ON CONFLICT (clct_dt, instllc_id) DO NOTHING";

        entityManager.createNativeQuery(query)
                .setParameter("clctDt", entity.getId().getCollectionDatetime())
                .setParameter("instllcId", entity.getId().getInstllcId())
                .setParameter("instllcNm", entity.getInstllcNm())
                .setParameter("instllcDescr", entity.getInstllcDescr())
                .setParameter("eqpmntId", entity.getEqpmntId())
                .setParameter("latitude", entity.getLatitude())
                .setParameter("longitude", entity.getLongitude())
                .executeUpdate();
    }
}