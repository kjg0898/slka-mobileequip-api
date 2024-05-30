package org.neighbor21.slkaMobileEquipApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLInsert;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.entity
 * fileName       : TL_MVMNEQ_PERIOD.java
 * author         : kjg08
 * date           : 24. 4. 29.
 * description    : 이동형장비 조사기간 정보 TL_MVMNEQ_PERIOD 테이블에 넣기 전 entity
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 29.        kjg08           최초 생성
 */
@Entity
@Getter
@Setter
@Table(name = "TL_MVMNEQ_PERIOD", schema = "srlk")
public class TL_MVMNEQ_PERIODEntity {

    @EmbeddedId
    private TL_MVMNEQ_PERIOD_IdEntity id;  // 사용할 복합 키

    // 시작 일시
    @Column(name = "START_DT")
    private LocalDateTime startTime;
    // 종료 일시
    @Column(name = "END_DT")
    private LocalDateTime  endTime;
}
