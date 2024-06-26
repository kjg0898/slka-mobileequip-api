package org.neighbor21.slkaMobileEquipApi.dto.listSite;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.dto.listSite
 * fileName       : ClassificationDTO.java
 * author         : kjg08
 * date           : 24. 4. 17.
 * description    : List Sites 장소목록 api 를 호출하여 나온 데이터를 받는 dto
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 17.        kjg08           최초 생성
 */
//class_scheme의 차량 클래스 목록
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationDTO {
    @JsonProperty("name")
    private String name;
}