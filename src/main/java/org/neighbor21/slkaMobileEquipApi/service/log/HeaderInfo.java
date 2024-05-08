package org.neighbor21.slkaMobileEquipApi.service.log;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.log
 * fileName       : HeaderInfo.java
 * author         : kjg08
 * date           : 24. 4. 22.
 * description    : api 호출 값 공통 헤더 부분 변수 선언 모음
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 22.        kjg08           최초 생성
 */

@Getter
@Setter
@ToString
public class HeaderInfo {
    String contentType;
    String contentLength;
    String connection;
    String date;
    String xAmznTraceId;
    String xCache;
    String via;
    String xAmzCfPop;
    String xAmzCfId;
}
