package org.neighbor21.slkaMobileEquipApi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import kong.unirest.*;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.service.log.LogService;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service
 * fileName       : MCATLYSTApiService.java
 * author         : kjg08
 * date           : 24. 4. 17.
 * description    : metrocount atlyst api 를 호출하여 데이터를 가져오고 dto 에 저장 후에 헤더 값을 로깅한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 17.        kjg08           최초 생성
 */
@Service
public class MCATLYSTApiService {
    private static final Logger logger = LoggerFactory.getLogger(MCATLYSTApiService.class);
    private final LogService logService = new LogService();
    private final Set<Integer> siteCache = new HashSet<>();  // 캐시 구조

    // API 호출 URL 설정파일에서 주입
    @Value("${api.url.list_sites}")
    private String listsitesApiUrl;

    @Value("${api.url.individual_vehicles}")
    private String individualvehiclesApiUrl;

    @Value("${api.key}")
    private String apiKey;

    @Autowired
    private Retry apiRetry;

    /**
     * List Sites 장소목록(모든 장소를 반환)
     *
     * @return List<ListSiteDTO> 장소목록 데이터
     * @throws UnirestException API 요청 시 발생하는 예외
     */
    public List<ListSiteDTO> listSites() throws UnirestException {

        // 실제 API 호출 부분 (주석 처리)
        //       return Retry.decorateCheckedSupplier(apiRetry, () -> {
//        try {
//            HttpResponse<String> response = Unirest.post(listsitesApiUrl)
//                    .header("APIKEY", apiKey)
//                    .body("")
//                    .asString();
//
//            logger.debug("List Sites 응답 바디: {}", response.getBody());
//            logger.debug("List Sites 응답 헤더: {}", response.getHeaders());
//            // 헤더값 로깅
//            logService.listSiteResponseHeaders(response);
//            // 응답 데이터 body 반환
//            if (response.getStatus() == 200) {
//                List<ListSiteDTO> sitesBody = new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
//                sitesBody.forEach(site -> {
//                    cacheSite(site.getSite_id());
//                    logger.info("응답 List Sites 데이터: {}", site);
//                });
//                return sitesBody;
//            } else {
//                logger.warn("장소 목록을 가져오는데 실패했습니다: HTTP {}", response.getStatus());
//                return Collections.emptyList();
//            }
//        } catch (JsonProcessingException e) {
//            logger.error("장소 목록 응답 파싱 중 오류 발생", e);
//            throw new RuntimeException("JSON 파싱 오류", e);
//        } catch (UnirestException e) {
//            logger.error("장소 데이터를 가져오는 중 오류 발생", e);
//            throw e;
//        }
//   }).apply();
//    }

        // 테스트 데이터 생성 부분
        try {
            // 여러 개의 테스트 데이터를 자동으로 생성
            String testData = generateTestData(800); // 원하는 개수만큼 생성

            HttpResponse<String> response = new HttpResponse<String>() {
                @Override
                public int getStatus() {
                    return 200; // HTTP 200 OK
                }

                @Override
                public String getStatusText() {
                    return "OK";
                }

                @Override
                public String getBody() {
                    return testData;
                }

                @Override
                public Headers getHeaders() {
                    Headers headers = new Headers();
                    headers.add("Content-Type", "application/json");
                    headers.add("Content-Length", String.valueOf(testData.length()));
                    headers.add("Connection", "keep-alive");
                    headers.add("Date", "Fri, 30 Jun 2023 01:27:21 GMT");
                    headers.add("x-amzn-RequestId", "e7a04d99-ef47-4ef1-a2c4-df098f049c8b");
                    headers.add("Content-Encoding", "gzip");
                    headers.add("Allow", "OPTIONS, POST");
                    headers.add("x-amzn-Remapped-Content-Length", "36296");
                    headers.add("X-Frame-Options", "SAMEORIGIN");
                    headers.add("x-amz-apigw-id", "HTxYFHLPIAMFQiA=");
                    headers.add("Vary", "Accept, Origin");
                    headers.add("X-Amzn-Trace-Id", "Root=1-649e2f66-3960974312a927dc7db721dd;Sampled=1;lineage=ebde1530:0");
                    headers.add("X-Cache", "Miss from cloudfront");
                    headers.add("Via", "1.1 52e479c500405e4e5b36d8a25429d06c.cloudfront.net (CloudFront)");
                    headers.add("X-Amz-Cf-Pop", "IAD55-P5");
                    headers.add("X-Amz-Cf-Id", "-ug4SvYW3oMWEV071hTQI5DDC8TyZTxNGDXkgWbTrZ_B4JkJD-HMrg==");
                    return headers;
                }

                @Override
                public boolean isSuccess() {
                    return getStatus() == 200;
                }

                @Override
                public <E> E mapError(Class<? extends E> errorClass) {
                    return null;
                }

                @Override
                public Cookies getCookies() {
                    return null;
                }

                @Override
                public Optional<UnirestParsingException> getParsingError() {
                    return null;
                }

                @Override
                public <V> V mapBody(Function<String, V> func) {
                    return null;
                }

                @Override
                public <V> HttpResponse<V> map(Function<String, V> func) {
                    return null;
                }

                @Override
                public HttpResponse<String> ifSuccess(Consumer<HttpResponse<String>> consumer) {
                    return null;
                }

                @Override
                public HttpResponse<String> ifFailure(Consumer<HttpResponse<String>> consumer) {
                    return null;
                }

                @Override
                public <E> HttpResponse<String> ifFailure(Class<? extends E> errorClass, Consumer<HttpResponse<E>> consumer) {
                    return null;
                }
            };

            logger.debug("List Sites 응답 바디: {}", response.getBody());
            logger.debug("List Sites 응답 헤더: {}", response.getHeaders());
            // 헤더값 로깅
            logService.listSiteResponseHeaders(response);
            // 응답 데이터 body 반환
            if (response.getStatus() == 200) {
                List<ListSiteDTO> sitesBody = new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {
                });
                sitesBody.forEach(site -> {
                    cacheSite(site.getSite_id());
                    // logger.info("응답 List Sites 데이터: {}", site);
                });
                return sitesBody;
            } else {
                logger.warn("장소 목록을 가져오는데 실패했습니다: HTTP {}", response.getStatus());
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            logger.error("장소 목록 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("JSON 파싱 오류", e);
        } catch (UnirestException e) {
            logger.error("장소 데이터를 가져오는 중 오류 발생", e);
            throw e;
        }
    }


    /**
     * Individual Vehicles 개별 차량(특정 장소에 대한 개별 차량기록)
     *
     * @param siteId 대상 장소 ID
     * @return List<IndividualVehiclesDTO> 개별 차량 기록 데이터
     * @throws UnirestException API 요청 시 발생하는 예외
     */
    public List<IndividualVehiclesDTO> individualVehicles(Integer siteId) throws UnirestException {
//        return Retry.decorateCheckedSupplier(apiRetry, () -> {
        // 이전 차량 지나간 시간
        Timestamp lastProcessedTime = VehicleUtils.LastVehiclePassTimeManager.getLastVehiclePassTime(siteId);
        String startTime = formatStartTime(lastProcessedTime);
        // 요청 파라미터 생성
        int limit = 10000;
        String VehiclesBody = buildRequestBody(siteId, startTime, limit);

        // 실제 API 호출 부분 (주석 처리)
//        try {
//            HttpResponse<String> response = Unirest.post(individualvehiclesApiUrl)
//                    .header("APIKEY", apiKey)
//                    .body(VehiclesBody)
//                    .asString();
//
//            logger.debug("Individual Vehicles 응답 바디: {}", response.getBody());
//            logger.debug("Individual Vehicles 응답 헤더: {}", response.getHeaders());
//            // 헤더 로그 로깅
//            logService.individualVehiclesResponseHeaders(response);
//            // 응답 데이터 body 반환
//            if (response.getStatus() == 200) {
//                List<IndividualVehiclesDTO> vehicles = new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
//                vehicles.forEach(vehicle -> vehicle.setSiteId(siteId));
//                logger.info("Individual vehicles 데이터: {}", vehicles);
//
//                return vehicles;
//            } else {
//                logger.warn("개별 차량 데이터를 가져오는데 실패했습니다: HTTP {}", response.getStatus());
//                return Collections.emptyList();
//            }
//        } catch (JsonProcessingException e) {
//            logger.error("개별 차량 응답 파싱 중 오류 발생", e);
//            throw new RuntimeException("JSON 파싱 오류", e);
//        } catch (UnirestException e) {
//            logger.error("API 요청 실패", e);
//            throw e;
//        }
//        }).apply();
//    }

        // 테스트 데이터 생성 부분
        try {
            String testData = generateIndividualVehiclesTestData(siteId, 1000);

            HttpResponse<String> response = new HttpResponse<String>() {
                @Override
                public int getStatus() {
                    return 200; // HTTP 200 OK
                }

                @Override
                public String getStatusText() {
                    return "OK";
                }

                @Override
                public String getBody() {
                    return testData;
                }

                @Override
                public Headers getHeaders() {
                    Headers headers = new Headers();
                    headers.add("Content-Type", "application/json");
                    headers.add("Content-Length", String.valueOf(testData.length()));
                    headers.add("Connection", "keep-alive");
                    headers.add("Date", "Fri, 30 Jun 2023 01:48:37 GMT");
                    headers.add("x-amzn-RequestId", "8bf6d870-0dd2-41f9-967d-4d65b94fc8ec");
                    headers.add("Content-Encoding", "gzip");
                    headers.add("Allow", "OPTIONS, POST");
                    headers.add("x-amzn-Remapped-Content-Length", "694770");
                    headers.add("X-Frame-Options", "SAMEORIGIN");
                    headers.add("x-amzn-apigw-id", "HT0hrGWmIAMFbgw=");
                    headers.add("Vary", "Accept, Origin");
                    headers.add("X-Amzn-Trace-Id", "Root=1-649e3470-0e13d5076746458a0d24307f;Sampled=1;lineage=ebde1530:0");
                    headers.add("X-Cache", "Miss from cloudfront");
                    headers.add("Via", "1.1 cf7e8b3887a490b60a55be14eb004b54.cloudfront.net (CloudFront)");
                    headers.add("X-Amz-Cf-Pop", "IAD55-P5");
                    headers.add("X-Amz-Cf-Id", "1x3CgByyXZ1wzq9-ImMd7fTQONJqm-m29OEnLBO8ZeF7oZpwfxYm3Q==");
                    return headers;
                }

                @Override
                public boolean isSuccess() {
                    return getStatus() == 200;
                }

                @Override
                public <E> E mapError(Class<? extends E> errorClass) {
                    return null;
                }

                @Override
                public Cookies getCookies() {
                    return null;
                }

                @Override
                public Optional<UnirestParsingException> getParsingError() {
                    return null;
                }

                @Override
                public <V> V mapBody(Function<String, V> func) {
                    return null;
                }

                @Override
                public <V> HttpResponse<V> map(Function<String, V> func) {
                    return null;
                }

                @Override
                public HttpResponse<String> ifSuccess(Consumer<HttpResponse<String>> consumer) {
                    return null;
                }

                @Override
                public HttpResponse<String> ifFailure(Consumer<HttpResponse<String>> consumer) {
                    return null;
                }

                @Override
                public <E> HttpResponse<String> ifFailure(Class<? extends E> errorClass, Consumer<HttpResponse<E>> consumer) {
                    return null;
                }
            };

            logger.debug("Individual Vehicles 응답 바디: {}", response.getBody());
            logger.debug("Individual Vehicles 응답 헤더: {}", response.getHeaders());
            // 헤더 로그 로깅
            logService.individualVehiclesResponseHeaders(response);
            // 응답 데이터 body 반환
            if (response.getStatus() == 200) {
                List<IndividualVehiclesDTO> vehicles = new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {
                });
                vehicles.forEach(vehicle -> vehicle.setSiteId(siteId));

                return vehicles;
            } else {
                logger.warn("개별 차량 데이터를 가져오는데 실패했습니다: HTTP {}", response.getStatus());
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            logger.error("개별 차량 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("JSON 파싱 오류", e);
        } catch (Exception e) {
            logger.error("개별 차량 데이터를 가져오는 중 오류 발생", e);
            throw new RuntimeException("API 호출 중 오류", e);
        }
    }

    /**
     * 요청 파라미터 생성
     *
     * @param siteId    장소 ID
     * @param startTime 시작 시간
     * @param limit     최대 반환 수
     * @return 요청 바디 문자열
     */
    private String buildRequestBody(Integer siteId, String startTime, int limit) {
        return String.format("{\"site_id\":%d,\"start_timestamp\":\"%s\",\"limit\":%d}", siteId, startTime, limit);
    }

    /**
     * 시작 시간 포맷
     *
     * @param timestamp 타임스탬프
     * @return 포맷된 시작 시간 문자열
     */
    private String formatStartTime(Timestamp timestamp) {
        if (timestamp != null) {
            return timestamp.toString().replace(" ", "T");
        } else {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    /**
     * individualVehicles API 호출할때 사용할 site_id 관리
     *
     * @param siteId 장소 ID
     */
    public void cacheSite(Integer siteId) {
        siteCache.add(siteId);
    }

    /**
     * 캐시가 비어있는지 확인
     *
     * @return 캐시가 비어있으면 true, 아니면 false
     */
    public boolean isCacheEmpty() {
        return siteCache.isEmpty();
    }

    /**
     * 안전하게 siteCache 접근
     *
     * @return 수정 불가능한 siteCache의 뷰
     */
    public Set<Integer> getSiteCache() {
        return Collections.unmodifiableSet(siteCache);
    }


    /**
     * 여러 개의 테스트 데이터를 생성하는 메소드
     *
     * @param count 생성할 데이터 개수
     * @return JSON 형식의 테스트 데이터 문자열
     */
    private static final Random random = new Random();

    private static String generateTestData(int count) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<String> testDataList = IntStream.range(0, count).mapToObj(i -> {
            Integer min = LocalTime.now().getMinute() + i; // Ensure IDs

            Integer siteId = i;
            String siteName = "SITE NAME " + min;
            String description = "EXAMPLE ROAD " + min;
            double latitude = -1.032914 + (random.nextDouble() * 0.01); // Slightly different latitude
            double longitude = 1.032914 + (random.nextDouble() * 0.01); // Slightly different longitude
            int assetManagementId = random.nextInt(36) + 1; // 1부터 36까지 랜덤 값 생성

            // Generate exactly 100 survey periods with random times for the same site_id
            List<String> surveyPeriods = IntStream.range(0, 100)
                    .mapToObj(j -> {
                        // Generate a start time between now - 5 days and now + 5 days
                        LocalDateTime startTime = LocalDateTime.now()
                                .minusDays(5)
                                .plusDays(random.nextInt(10))
                                .plusMinutes(j * 10L);
                        LocalDateTime endTime = startTime.plusWeeks(2);
                        return String.format("{\"start_time\": \"%s\", \"end_time\": \"%s\"}",
                                startTime.format(formatter), endTime.format(formatter));
                    })
                    .collect(Collectors.toList());

            return String.format("{\"pk\": %d, \"site_id\": %d, \"name\": \"%s\", \"description\": \"%s\", \"latitude\": %f, \"longitude\": %f, \"asset_management_id\": \"%d\", \"class_scheme_name\": \"VRX\", \"survey_periods\": [%s], \"classifications\": [{\"name\": \"AR0\"}, {\"name\": \"SV\"}]}",
                    i, siteId, siteName, description, latitude, longitude, assetManagementId, String.join(",", surveyPeriods));
        }).collect(Collectors.toList());

        return "[" + String.join(",", testDataList) + "]";
    }

    /**
     * 여러 개의 테스트 Individual Vehicles 데이터를 생성하는 메소드
     *
     * @param siteId 장소 ID
     * @param count  생성할 데이터 개수
     * @return JSON 형식의 테스트 데이터 문자열
     */
    public String generateIndividualVehiclesTestData(int siteId, int count) {
        List<Map<String, Object>> dataList = IntStream.range(0, count).mapToObj(i -> {
            Map<String, Object> data = new HashMap<>();
            LocalDateTime timestamp = LocalDateTime.now(ZoneId.of("Asia/Colombo")).truncatedTo(ChronoUnit.SECONDS).minusMinutes(count - i); // Ensure unique timestamps
            data.put("site_id", siteId);
            data.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            data.put("localtime", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            data.put("heading", i % 2 == 0 ? "North" : "South");
            data.put("velocity(m/s)", BigDecimal.valueOf(Math.random() * 10));
            data.put("length(m)", BigDecimal.valueOf(Math.random() * 2));
            data.put("headway(s)", (int) (Math.random() * 100));
            data.put("class_scheme(Shared Path 02)", i % 3 == 0 ? "Short Cycle" : "Long Cycle");
            data.put("lane_index", i % 2);
            return data;
        }).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(dataList);
            logger.debug("Generated test data: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error("테스트 데이터 생성 중 오류 발생", e);
            throw new RuntimeException("JSON 생성 오류", e);
        }
    }
}
