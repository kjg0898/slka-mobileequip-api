package org.neighbor21.slkaMobileEquipApi.service.log;

import kong.unirest.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.log
 * fileName       : LogService.java
 * author         : kjg08
 * date           : 24. 4. 19.
 * description    : 메트로 카운트 api 호출시 받아오는 헤더 정보 로깅
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 19.        kjg08           최초 생성
 */
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    // 헤더 로그 공통 부분
    public HeaderInfo commonResponseHeaders(HttpResponse<String> response) {
        HeaderInfo info = new HeaderInfo();
        try {
            // 응답 데이터 형식
            info.contentType = Optional.ofNullable(response.getHeaders().getFirst("Content-Type")).orElse("Unknown Content-Type");
            // 응답 본문의 길이
            info.contentLength = Optional.ofNullable(response.getHeaders().getFirst("Content-Length")).orElse("Unknown Content-Length");
            // 연결 상태
            info.connection = Optional.ofNullable(response.getHeaders().getFirst("Connection")).orElse("Unknown Connection");
            // 응답이 생성된 정확한 시간
            info.date = Optional.ofNullable(response.getHeaders().getFirst("Date")).orElse("Unknown Date");

            //Amazon Web Services에서 생성한 특정 요청 ID로, 요청을 추적하고 문제를 진단하는 데 사용될 수 있습니다.
            info.xAmznTraceId = Optional.ofNullable(response.getHeaders().getFirst("X-Amzn-Trace-Id")).orElse("No AWS Request ID");
            //ex) Miss from cloudfront: CloudFront 캐시에서 응답이 캐시 미스임을 나타내고, 오리진 서버에서 직접 응답이 제공되었음을 알려줍니다.
            info.xCache = Optional.ofNullable(response.getHeaders().getFirst("X-Cache")).orElse("Unknown Cache Status");
            //ex) 1.1 52e479c500405e4e5b36d8a25429d06c.cloudfront.net (CloudFront): 응답이 Amazon CloudFront를 통해 전송되었음을 나타냅니다.
            info.via = Optional.ofNullable(response.getHeaders().getFirst("Via")).orElse("Unknown Via");
            //CloudFront의 Point of Presence(POP) 위치를 나타냅니다.
            info.xAmzCfPop = Optional.ofNullable(response.getHeaders().getFirst("X-Amz-Cf-Pop")).orElse("Unknown CloudFront POP");
            //CloudFront 요청 ID로, 요청을 CloudFront 내에서 추적하는 데 사용됩니다.
            info.xAmzCfId = Optional.ofNullable(response.getHeaders().getFirst("X-Amz-Cf-Id")).orElse("Unknown CloudFront ID");

            //info 로그 출력
            logger.info("data type : {}, body length : {}, connection type : {}, The time the response was generated : {}",
                    info.contentType, info.contentLength, info.connection, info.date);
            return info;
        } catch (Exception e) {
            logger.error("Error processing response headers", e);
        }
        return info;
    }


    // List Sites 용 헤더 로그 메소드
    public void listSiteResponseHeaders(HttpResponse<String> response) {

        HeaderInfo headerInfo = commonResponseHeaders(response);

        logger.debug("AWS 요청 추적 헤더 : {}, 응답이 캐시에서 제공되었는지(Hit), 아니면 오리진 서버에서 직접 제공되었는지(Miss) : {}, " +
                        "요청이나 응답이 전송되는 과정에서 거쳐간 중개 서버의 정보 : {}, CloudFront Point of Presence(POP)의 위치 : {}, " +
                        "CloudFront 내에서 요청을 추적하는 데 사용되는 고유 식별자 : {}",
                headerInfo.xAmznTraceId, headerInfo.xCache, headerInfo.via, headerInfo.xAmzCfPop, headerInfo.xAmzCfId);
    }

    //Individual Vehicles 개별 차량(특정 장소에 대한 개별 차량기록)
    public void individualVehiclesResponseHeaders(HttpResponse<String> response) {

        // Amazon Web Services에서 생성한 특정 요청 ID로, 요청을 추적하고 문제를 진단하는 데 사용
        String xAmznRequestId = Optional.ofNullable(response.getHeaders().getFirst("x-amzn-RequestId")).orElse("No AWS Request ID");
        HeaderInfo headerInfo = commonResponseHeaders(response);

        logger.debug("Amazon Web Services에서 생성한 특정 요청 ID : {}, AWS 요청 추적 헤더 : {}, 응답이 캐시에서 제공되었는지(Hit) 아니면 오리진 서버에서 직접 제공되었는지(Miss) : {}, " +
                        "요청이나 응답이 전송되는 과정에서 거쳐간 중개 서버의 정보 : {}, CloudFront Point of Presence(POP)의 위치 : {}, " +
                        "CloudFront 내에서 요청을 추적하는 데 사용되는 고유 식별자 : {}",
                xAmznRequestId, headerInfo.xAmznTraceId, headerInfo.xCache, headerInfo.via, headerInfo.xAmzCfPop, headerInfo.xAmzCfId);
    }
}
