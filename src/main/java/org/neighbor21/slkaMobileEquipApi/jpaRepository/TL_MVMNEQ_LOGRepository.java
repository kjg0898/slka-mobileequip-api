package org.neighbor21.slkaMobileEquipApi.jpaRepository;

import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_LOGEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_LOG_IdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.jpaRepository
 * fileName       : TL_MVMNEQ_LOGRepository.java
 * author         : kjg08
 * date           : 24. 4. 24.
 * description    : TL_MVMNEQ_LOG 테이블에 넣기 위한 리파지토리 인터페이스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 24.        kjg08           최초 생성
 */
public interface TL_MVMNEQ_LOGRepository extends JpaRepository<TL_MVMNEQ_LOGEntity, TL_MVMNEQ_LOG_IdEntity> {

}
