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
 * fileName       : TL_MVMNEQ_PERIOD_Id.java
 * author         : kjg08
 * date           : 24. 4. 29.
 * description    :이동형장비 조사기간 정보 TL_MVMNEQ_PERIOD 의 pk (복합키)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 29.        kjg08           최초 생성
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class TL_MVMNEQ_PERIOD_IdEntity implements Serializable {
    // 수집 일시(now)
    @Column(name = "CLCT_DT")
    private Timestamp collectionDatetime;

    // 순번
    @Column(name = "SQNO", precision = 5, scale = 0)
    private Integer sequenceNo;

    // 설치 위치 아이디
    @Column(name = "INSTLLC_ID", length = 32)
    private String instllcId;


    // Default constructor
    public TL_MVMNEQ_PERIOD_IdEntity() {
    }

    // Constructor
    public TL_MVMNEQ_PERIOD_IdEntity(Timestamp collectionDatetime, Integer sequenceNo, String instllcId) {
        this.collectionDatetime = collectionDatetime;
        this.sequenceNo = sequenceNo;
        this.instllcId = instllcId;
    }
}