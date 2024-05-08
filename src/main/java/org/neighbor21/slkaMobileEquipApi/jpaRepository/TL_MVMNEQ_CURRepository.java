package org.neighbor21.slkaMobileEquipApi.jpaRepository;

import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_CUREntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.jpa.repository
 * fileName       : MCATLYSTRepository.java
 * author         : kjg08
 * date           : 24. 4. 19.
 * description    : TL_MVMNEQ_CUR 테이블에 넣기 위한 리파지토리 인터페이스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 19.        kjg08           최초 생성
 */
public interface TL_MVMNEQ_CURRepository extends JpaRepository<TL_MVMNEQ_CUREntity, String> {

    // TL_MVMNEQ_CUR 테이블에 장비 아이디 값을 조회하기 위해 생성
    Optional<TL_MVMNEQ_CUREntity> findByEqpmntId(String equipmentId);

}
