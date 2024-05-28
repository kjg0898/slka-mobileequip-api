package org.neighbor21.slkaMobileEquipApi.jpaRepository;

import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.jpaRepository
 * fileName       : TL_MVMNEQ_PERIODRepository.java
 * author         : kjg08
 * date           : 24. 4. 29.
 * description    :TL_MVMNEQ_PERIOD 테이블에 넣기 위한 리파지토리 인터페이스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 29.        kjg08           최초 생성
 */
public interface TL_MVMNEQ_PERIODRepository extends JpaRepository<TL_MVMNEQ_PERIODEntity, TL_MVMNEQ_PERIOD_IdEntity> {



    // 네이티브 쿼리를 사용하여 해당 instllcId 중 가장 높은 순번값 가져옴
    @Query(value = "SELECT COALESCE(MAX(sqno), 0) FROM srlk.tl_mvmneq_period WHERE instllc_id = :instllcId", nativeQuery = true)
    Integer findMaxSequenceNoByInstllcId(@Param("instllcId") String instllcId);
}
