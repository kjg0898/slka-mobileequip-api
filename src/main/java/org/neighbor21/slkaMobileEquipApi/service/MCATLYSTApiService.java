package org.neighbor21.slkaMobileEquipApi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.service.log.LogService;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * List Sites 장소목록(모든 장소를 반환)
     *
     * @return List<ListSiteDTO> 장소목록 데이터
     * @throws UnirestException API 요청 시 발생하는 예외
     */
    public List<ListSiteDTO> listSites() throws UnirestException {
        try {
            HttpResponse<String> response = Unirest.post(listsitesApiUrl)
                    .header("APIKEY", apiKey)
                    .body("")
                    .asString();
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
                    logger.info("응답 List Sites 데이터: {}", site);
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
        // 이전 차량 지나간 시간
        Timestamp lastProcessedTime = VehicleUtils.LastVehiclePassTimeManager.getLastVehiclePassTime(siteId);
        String startTime = formatStartTime(lastProcessedTime);
        // 요청 파라미터 생성
        int limit = 10000;
        String VehiclesBody = buildRequestBody(siteId, startTime, limit);

        try {
            HttpResponse<String> response = Unirest.post(individualvehiclesApiUrl)
                    .header("APIKEY", apiKey)
                    .body(VehiclesBody)
                    .asString();
            logger.debug("Individual Vehicles 응답 바디: {}", response.getBody());
            logger.debug("Individual Vehicles 응답 헤더: {}", response.getHeaders());
            // 헤더 로그 로깅
            logService.individualVehiclesResponseHeaders(response);
            // 응답 데이터 body 반환
            if (response.getStatus() == 200) {
                List<IndividualVehiclesDTO> vehicles = new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {
                });
                vehicles.forEach(vehicle -> vehicle.setSiteId(siteId));
                logger.info("Individual vehicles 데이터: {}", vehicles);
                return vehicles;
            } else {
                logger.warn("개별 차량 데이터를 가져오는데 실패했습니다: HTTP {}", response.getStatus());
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            logger.error("개별 차량 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("JSON 파싱 오류", e);
        } catch (UnirestException e) {
            logger.error("API 요청 실패", e);
            throw e;
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
        logger.info("사이트 ID 캐싱: {}", siteId);
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
}