package org.neighbor21.slkaMobileEquipApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLInsert;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_CUR_IdEntity;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity
 * fileName       : TL_MVMNEQ_CUR.java
 * author         : kjg08
 * date           : 24. 4. 18.
 * description    : 이동형장비 설치위치 관리 (얘는 최신 위치만 관리 하므로 업데이트) TL_MVMNEQ_CUR 테이블에 넣기 전 엔티티 하이버네티스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 18.        kjg08           최초 생성
 */
@Entity
@Getter
@Setter
@Table(name = "TL_MVMNEQ_CUR", schema = "srlk")
@SQLInsert(sql = "INSERT INTO srlk.tl_mvmneq_cur (clct_dt, eqpmnt_id, instllc_descr, instllc_nm, lat, lon, instllc_id) VALUES (?, ?, ?, ?, ?, ?, ?) " +
        "ON CONFLICT (instllc_id) DO UPDATE SET eqpmnt_id = EXCLUDED.eqpmnt_id, instllc_descr = EXCLUDED.instllc_descr, instllc_nm = EXCLUDED.instllc_nm, lat = EXCLUDED.lat, lon = EXCLUDED.lon, clct_dt = EXCLUDED.clct_dt")
public class TL_MVMNEQ_CUREntity {

    @EmbeddedId
    private TL_MVMNEQ_CUR_IdEntity instllcId;  // 사용할 복합 키

    @Column(name = "CLCT_DT")
    private Timestamp CollectionDatetime; // 수집일시

    @Column(name = "EQPMNT_ID", length = 50)
    private String eqpmntId;  // 장비 아이디

    @Column(name = "INSTLLC_DESCR", length = 400)
    private String instllcDescr;  // 설치위치 설명

    @Column(name = "INSTLLC_NM", length = 50)
    private String instllcNm;  // 설치위치 명

    @Column(name = "LAT", precision = 14, scale = 8)
    private BigDecimal latitude;  // 위도

    @Column(name = "LON", precision = 14, scale = 8)
    private BigDecimal longitude;  // 경도
}
