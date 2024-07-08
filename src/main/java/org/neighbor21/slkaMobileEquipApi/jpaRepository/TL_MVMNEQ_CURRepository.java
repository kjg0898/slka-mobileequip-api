package org.neighbor21.slkaMobileEquipApi.jpaRepository;

import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_CUREntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_CUR_IdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.jpa.repository
 * fileName       : TL_MVMNEQ_CURRepository.java
 * author         : kjg08
 * date           : 24. 4. 19.
 * description    : TL_MVMNEQ_CUR 테이블에 넣기 위한 리포지토리 인터페이스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 19.        kjg08           최초 생성
 */
public interface TL_MVMNEQ_CURRepository extends JpaRepository<TL_MVMNEQ_CUREntity, TL_MVMNEQ_CUR_IdEntity> {

}
