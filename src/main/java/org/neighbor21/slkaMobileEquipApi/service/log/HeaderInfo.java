package org.neighbor21.slkaMobileEquipApi.service.log;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.log
 * fileName       : HeaderInfo.java
 * author         : kjg08
 * date           : 24. 4. 22.
 * description    : API 호출 시 공통으로 사용되는 헤더 정보를 저장하는 클래스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 22.        kjg08           최초 생성
 * 24. 5. 17.        kjg08            주석 추가 및 description 갱신
 */

@Getter
@Setter
@ToString
public class HeaderInfo {
    // HTTP Content-Type 헤더 값
    private String contentType;

    // HTTP Content-Length 헤더 값
    private String contentLength;

    // HTTP Connection 헤더 값
    private String connection;

    // HTTP Date 헤더 값
    private String date;

    // AWS X-Amzn-Trace-Id 헤더 값
    private String xAmznTraceId;

    // HTTP X-Cache 헤더 값
    private String xCache;

    // HTTP Via 헤더 값
    private String via;

    // AWS X-Amz-Cf-Pop 헤더 값
    private String xAmzCfPop;

    // AWS X-Amz-Cf-Id 헤더 값
    private String xAmzCfId;
}