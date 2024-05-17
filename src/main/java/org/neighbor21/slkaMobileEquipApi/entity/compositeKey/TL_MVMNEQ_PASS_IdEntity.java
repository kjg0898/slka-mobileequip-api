package org.neighbor21.slkaMobileEquipApi.entity.compositeKey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity.compositeKey
 * fileName       : TL_MVMNEQ_PASS_Id.java
 * author         : kjg08
 * date           : 24. 4. 22.
 * description    :이동형장비 통과차량 정보 TL_MVMNEQ_PASS 의 pk @id 어노테이션으로는 하나의 pk 밖에 지정할수 없으므로 대신에 복합 pk 구조를 미리 정의해둚
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 22.        kjg08           최초 생성
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class TL_MVMNEQ_PASS_IdEntity implements Serializable {
    // 통행 일시
    @Column(name = "PASS_DT")
    private Timestamp passTime;

    // 차량 방향
    @Column(name = "VHCL_DRCT", length = 30)
    private String vehicleDirection;

    // 통행 차로
    @Column(name = "PASS_LANE", precision = 10, scale = 0)
    private Integer passLane;

    // 설치 위치 아이디
    @Column(name = "INSTLLC_ID", length = 32)
    private String instllcId;


    // Default constructor
    public TL_MVMNEQ_PASS_IdEntity() {
    }

//    // Constructor
//    public TL_MVMNEQ_PASS_IdEntity(Timestamp passTime, String vehicleDirection, int passLane, String instllcId) {
//        this.passTime = passTime;
//        this.vehicleDirection = vehicleDirection;
//        this.passLane = passLane;
//        this.instllcId = instllcId;
//    }
}