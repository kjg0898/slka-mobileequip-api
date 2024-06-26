package org.neighbor21.slkaMobileEquipApi.dto.listSite;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.dto.listSite
 * fileName       : ListSiteDTO.java
 * author         : kjg08
 * date           : 24. 4. 17.
 * description    : List Sites 장소목록 api 를 호출하여 나온 데이터를 받는 dto
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 17.        kjg08           최초 생성
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListSiteDTO {

    //pk
    @JsonProperty("pk")
    private int pk;

    // 추가 요청에 필요한 고유 장소 ID
    @JsonProperty("site_id")
    private Integer site_id;
    //장소 이름
    @JsonProperty("name")
    private String name;
    //장소 설명
    @JsonProperty("description")
    private String description;
    //
    @JsonProperty("latitude")
    private Float latitude;
    //
    @JsonProperty("longitude")
    private Float longitude;
    //자산관리 ID
    @JsonProperty("asset_management_id")
    private String asset_management_id;
    // 현장에서 적용된 차량 분류 체계
    @JsonProperty("class_scheme_name")
    private String class_scheme_name;
    // class_scheme 의 차량 클래스 목록
    private List<ClassificationDTO> classifications;
    // ISO-8601 표준의 조사 시작 시간 및 종료 시간(YYYY-MM-DDTHH:MM:SS+tz 형식)
    //interval_statistics의 시작 시간, 종료 시간 결정
    //일시적으로 데이터 수집하는 장소의 경우 각 설문조사 기간마다 survey_periods 배열에 별도로 추가
    //영구적으로 수집하는 장소는 survey_periods 배열에 해당하는 요소에 새 데이터가 들어올 때마다 설문조사 기간의 종료 시간 업데이트함
    private List<SurveyPeriodDTO> survey_periods;
}