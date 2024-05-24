package org.neighbor21.slkaMobileEquipApi.service.conversion;


import jakarta.persistence.EntityManager;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PASSEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PASS_IdEntity;
import org.neighbor21.slkaMobileEquipApi.service.BatchService;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.conversion
 * fileName       : VehiclePassService.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    : 개별 차량 통과 정보를 처리하고 저장하는 서비스 클래스.
 * 이 클래스는 개별 차량 통과 데이터를 받아와서 데이터베이스에 저장하는 작업을 수행한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
 */

@Service
public class VehiclePassService {

    private static final Logger logger = LoggerFactory.getLogger(VehiclePassService.class);

    // siteId별 마지막 통과 시간 저장
    private final Map<Integer, Timestamp> lastPassTimeMap = new ConcurrentHashMap<>();

    @Autowired
    private BatchService batchService;

    /**
     * 개별 차량 통과 정보를 받아와서 데이터베이스에 저장하는 메소드.
     *
     * @param vehicles List<IndividualVehiclesDTO> 개별 차량 정보 리스트
     */
    @Transactional
    public void saveVehiclePasses(List<IndividualVehiclesDTO> vehicles) {
        List<TL_MVMNEQ_PASSEntity> passEntities = new ArrayList<>();
        vehicles.forEach(vehicle -> {
            try {
                Integer siteId = vehicle.getSiteId();
                // 마지막 통과 시간 가져오기, 없으면 현재 시간
                Timestamp lastPassTime = lastPassTimeMap.getOrDefault(siteId, new Timestamp(System.currentTimeMillis()));
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
                logger.info("TL_MVMNEQ_PASS 엔티티에 삽입 성공 (장소 ID: {}): {}", siteId, tlMvmneqPassEntity);
            } catch (Exception e) {
                logger.error("TL_MVMNEQ_PASS 처리 중 오류 발생", e);
            }
        });

        // 엔티티 리스트를 배치로 삽입
        try {
            batchService.batchInsertWithRetry(passEntities, this::insertEntity);
            logger.info("TL_MVMNEQ_PASS 배치 삽입 완료.");
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PASS 배치 삽입 실패", e);
        }
    }

    private <T> void insertEntity(EntityManager entityManager, T entity) {
        entityManager.persist(entity);
    }
}
