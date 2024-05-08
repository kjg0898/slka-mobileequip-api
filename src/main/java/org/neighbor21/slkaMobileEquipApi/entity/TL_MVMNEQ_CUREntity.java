package org.neighbor21.slkaMobileEquipApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity
 * fileName       : TL_MVMNEQ_CUR.java
 * author         : kjg08
 * date           : 24. 4. 18.
 * description    : 이동형장비 설치위치 관리 (얘는 최신 위치만 관리 하므로 업데이트) TL_MVMNEQ_CUR  테이블에 넣기 전 엔티티
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 18.        kjg08           최초 생성
 */
@Entity
@Getter
@Setter
@Table(name = "TL_MVMNEQ_CUR")
public class TL_MVMNEQ_CUREntity {


    @Id
    @Column(name = "INSTLLC_ID", length = 32)
    private String instllcId;  // 설치위치 아이디

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
