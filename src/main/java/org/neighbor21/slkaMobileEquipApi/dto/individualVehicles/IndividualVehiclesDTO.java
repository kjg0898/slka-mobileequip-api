package org.neighbor21.slkaMobileEquipApi.dto.individualVehicles;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.dto.individualVehicles
 * fileName       : VehicleDataDTO.java
 * author         : kjg08
 * date           : 24. 4. 19.
 * description    :Individual Vehicles 개별 차량 (특정 장소에 대한 개별 차량기록) api 호출 하여 나온 데이터를 받는 dto
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 19.        kjg08           최초 생성
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualVehiclesDTO {
    //Individual Vehicles 조회할때 사용한 listSite 에서 가져온 고유 장소 ID
    @JsonProperty("site_id")
    private Integer siteId;

    //ISO-8601 표준의 UTC 시간(YYYY-MM-DDTHH:MM:SS.ss+tz 형식)
    @JsonProperty("timestamp")
    private Timestamp timestamp;

    //차량이 ISO-8601 표준으로 기록된 시간(YYYY-MM-DDTHH:MM:SS.ss 형식)
    @JsonProperty("localtime")
    private Timestamp localtime;

    //차량의 방향(예: "북쪽", "남쪽")
    @JsonProperty("heading")
    private String heading;

    //기록된 차량 속도(m/s)
    @JsonProperty("velocity(m/s)")
    private BigDecimal velocity;

    //기록된 차량 길이(미터)
    @JsonProperty("length(m)")
    private BigDecimal length;

    //기록된 차량 진행 속도(초)
    @JsonProperty("headway(s)")
    private Integer headway;

    //장소 차량 체계 내에서 식별된 차량 클래스
    @JsonProperty("class_scheme(Shared Path 02)")
    private String vehicleClass;

    //차량이 기록된 차선을 나타내는 정수 값
    //  ㄴ 실시간 모니터링의 경우, 차량이 같은 차선에 있는지 "lane_index"와 "방향"을 결합하여 확인가능(예: "North" 방향과 lane_index 1은 "North - 1" 차선을 의미).
    //  ㄴ 다른 장소의 경우 lane_index는 신뢰할 수 없으므로 사용해서는 안됨
    @JsonProperty("lane_index")
    private Integer laneIndex;
}
