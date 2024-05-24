package org.neighbor21.slkaMobileEquipApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PASS_IdEntity;

import java.math.BigDecimal;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity
 * fileName       : TL_MVMNEQ_PASS.java
 * author         : kjg08
 * date           : 24. 4. 22.
 * description    : 이동형장비 통과차량 정보 TL_MVMNEQ_PASS 테이블에 넣기 전 entity
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 22.        kjg08           최초 생성
 */
@Entity
@Getter
@Setter
@Table(name = "TL_MVMNEQ_PASS", schema = "srlk")
public class TL_MVMNEQ_PASSEntity {

    @EmbeddedId
    private TL_MVMNEQ_PASS_IdEntity id;  // 사용할 복합 키

    @Column(name = "vehicle_speed", precision = 5, scale = 2)
    private BigDecimal vehicleSpeed;  // 차량 속도

    @Column(name = "vehicle_length", precision = 9, scale = 2)
    private BigDecimal vehicleLength;  // 차량 길이

    @Column(name = "vehicle_headway", precision = 5)
    private int vehicleHeadway;  // 차량 간격 초

    @Column(name = "vehicle_class", length = 30)
    private String vehicleClass;  // 차량 분류
}