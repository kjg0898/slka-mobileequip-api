package org.neighbor21.slkaMobileEquipApi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_LOG_IdEntity;

import java.math.BigDecimal;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity
 * fileName       : TL_MVMNEQ_LOG.java
 * author         : kjg08
 * date           : 24. 4. 24.
 * description    : 이동형장비 설치위치 이력 관리 (얘는 이력이므로  TL_MVMNEQ_CUR 테이블의 내용이 변경 되었을때 이력 추가) TL_MVMNEQ_LOG  테이블에 넣기 전 엔티티
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 24.        kjg08           최초 생성
 */
@Entity
@Getter
@Setter
@Table(name = "TL_MVMNEQ_LOG")
public class TL_MVMNEQ_LOGEntity {

    @EmbeddedId
    private TL_MVMNEQ_LOG_IdEntity id;  // 사용할 복합 키

    @Column(name = "INSTLLC_NM", length = 50)
    private String instllcNm;  // 설치위치 명

    @Column(name = "INSTLLC_DESCR", length = 400)
    private String instllcDescr;  // 설치위치 설명

    @Column(name = "EQPMNT_ID", length = 50)
    private String eqpmntId;  // 장비 아이디

    @Column(name = "latitude", precision = 14, scale = 8)
    private BigDecimal latitude;  // 위도

    @Column(name = "longitude", precision = 14, scale = 8)
    private BigDecimal longitude;  // 경도









}
