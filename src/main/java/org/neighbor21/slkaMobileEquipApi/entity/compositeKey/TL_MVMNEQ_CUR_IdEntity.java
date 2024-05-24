package org.neighbor21.slkaMobileEquipApi.entity.compositeKey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity.compositeKey
 * fileName       : TL_MVMNEQ_CUR_IdEntity.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class TL_MVMNEQ_CUR_IdEntity implements Serializable {

    // 설치위치 아이디
    @Column(name = "INSTLLC_ID", length = 32)
    private String instllcId;

    // Default constructor
    public TL_MVMNEQ_CUR_IdEntity() {
    }


    // Constructor
    public TL_MVMNEQ_CUR_IdEntity(String instllcId) {
        this.instllcId = instllcId;
    }

}
