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
 * description    : 메트로 카운트 API 호출 시 받아오는 헤더 정보를 로깅하는 서비스 클래스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 19.        kjg08           최초 생성
 * 24. 5. 17.        kjg08            주석 추가 및 description 갱신
 */
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    /**
     * 공통 응답 헤더 정보를 로그로 기록하고, HeaderInfo 객체를 반환한다.
     *
     * @param response HttpResponse 객체
     * @return HeaderInfo 객체
     */
    public HeaderInfo commonResponseHeaders(HttpResponse<String> response) {
        HeaderInfo info = new HeaderInfo();
        try {
            // 응답 데이터 형식
            info.setContentType(getHeader(response, "Content-Type", "Unknown Content-Type"));
            // 응답 본문의 길이
            info.setContentLength(getHeader(response, "Content-Length", "Unknown Content-Length"));
            // 연결 상태
            info.setConnection(getHeader(response, "Connection", "Unknown Connection"));
            // 응답이 생성된 정확한 시간
            info.setDate(getHeader(response, "Date", "Unknown Date"));

            // Amazon Web Services에서 생성한 특정 요청 ID로, 요청을 추적하고 문제를 진단하는 데 사용될 수 있습니다.
            info.setXAmznTraceId(getHeader(response, "X-Amzn-Trace-Id", "No AWS Request ID"));
            // 예: Miss from cloudfront: CloudFront 캐시에서 응답이 캐시 미스임을 나타내고, 오리진 서버에서 직접 응답이 제공되었음을 알려줍니다.
            info.setXCache(getHeader(response, "X-Cache", "Unknown Cache Status"));
            // 예: 1.1 52e479c500405e4e5b36d8a25429d06c.cloudfront.net (CloudFront): 응답이 Amazon CloudFront를 통해 전송되었음을 나타냅니다.
            info.setVia(getHeader(response, "Via", "Unknown Via"));
            // CloudFront의 Point of Presence(POP) 위치를 나타냅니다.
            info.setXAmzCfPop(getHeader(response, "X-Amz-Cf-Pop", "Unknown CloudFront POP"));
            // CloudFront 요청 ID로, 요청을 CloudFront 내에서 추적하는 데 사용됩니다.
            info.setXAmzCfId(getHeader(response, "X-Amz-Cf-Id", "Unknown CloudFront ID"));

            // info 로그 출력
            logger.debug("data type : {}, body length : {}, connection type : {}, The time the response was generated : {}",
                    info.getContentType(), info.getContentLength(), info.getConnection(), info.getDate());
            return info;
        } catch (Exception e) {
            logger.error("Error processing response headers", e);
            // 예외 발생 시 추가적인 예외 처리를 수행할 수 있습니다. 예: 알림 발송, 상태 업데이트 등
        }
        return info;
    }

    /**
     * HttpResponse 객체에서 특정 헤더 값을 가져오는 메서드.
     *
     * @param response     HttpResponse 객체
     * @param headerName   헤더 이름
     * @param defaultValue 기본값
     * @return 헤더 값
     */
    private String getHeader(HttpResponse<String> response, String headerName, String defaultValue) {
        return Optional.ofNullable(response.getHeaders().getFirst(headerName)).orElse(defaultValue);
    }

    /**
     * List Sites API 응답 헤더를 로그로 기록하는 메서드.
     *
     * @param response HttpResponse 객체
     */
    public void listSiteResponseHeaders(HttpResponse<String> response) {
        HeaderInfo headerInfo = commonResponseHeaders(response);

        logger.debug("AWS 요청 추적 헤더 : {}, 응답이 캐시에서 제공되었는지(Hit), 아니면 오리진 서버에서 직접 제공되었는지(Miss) : {}, " +
                        "요청이나 응답이 전송되는 과정에서 거쳐간 중개 서버의 정보 : {}, CloudFront Point of Presence(POP)의 위치 : {}, " +
                        "CloudFront 내에서 요청을 추적하는 데 사용되는 고유 식별자 : {}",
                headerInfo.getXAmznTraceId(), headerInfo.getXCache(), headerInfo.getVia(), headerInfo.getXAmzCfPop(), headerInfo.getXAmzCfId());
    }

    /**
     * Individual Vehicles API 응답 헤더를 로그로 기록하는 메서드.
     *
     * @param response HttpResponse 객체
     */
    public void individualVehiclesResponseHeaders(HttpResponse<String> response) {
        // Amazon Web Services에서 생성한 특정 요청 ID로, 요청을 추적하고 문제를 진단하는 데 사용
        String xAmznRequestId = Optional.ofNullable(response.getHeaders().getFirst("x-amzn-RequestId")).orElse("No AWS Request ID");
        HeaderInfo headerInfo = commonResponseHeaders(response);

        logger.debug("Amazon Web Services에서 생성한 특정 요청 ID : {}, AWS 요청 추적 헤더 : {}, 응답이 캐시에서 제공되었는지(Hit) 아니면 오리진 서버에서 직접 제공되었는지(Miss) : {}, " +
                        "요청이나 응답이 전송되는 과정에서 거쳐간 중개 서버의 정보 : {}, CloudFront Point of Presence(POP)의 위치 : {}, " +
                        "CloudFront 내에서 요청을 추적하는 데 사용되는 고유 식별자 : {}",
                xAmznRequestId, headerInfo.getXAmznTraceId(), headerInfo.getXCache(), headerInfo.getVia(), headerInfo.getXAmzCfPop(), headerInfo.getXAmzCfId());
    }
}
