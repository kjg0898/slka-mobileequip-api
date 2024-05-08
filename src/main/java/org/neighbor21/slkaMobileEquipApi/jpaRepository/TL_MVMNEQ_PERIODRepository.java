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

    // TL_MVMNEQ_PERIOD 테이블에서 해당하는 instllcId 중의 최신 startTime 값 이 들어있는 릴레이션 의 순번(SQNO) 값을 가져오는 쿼리
    @Query("SELECT COALESCE(MAX(p.id.sequenceNo), 0) FROM TL_MVMNEQ_PERIODEntity p WHERE p.startTime = :startTime AND p.id.instllcId = :instllcId")
    Integer findMaxSequenceNoByInstllcId(@Param("startTime") Timestamp startTime, @Param("instllcId") String instllcId);
}
