package org.neighbor21.slkaMobileEquipApi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.Getter;
import org.neighbor21.slkaMobileEquipApi.dto.individualVehicles.IndividualVehiclesDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.service.log.LogService;
import org.neighbor21.slkaMobileEquipApi.service.util.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private LogService logService = new LogService();
    private Set<Integer> siteCache = new HashSet<>();  // 캐시 구조
    private Integer limit = 10000;

    public MCATLYSTApiService() {
        //서비스 인스턴스화 시 Unirest 구성 재설정
        resetUnirestConfig();
    }


    private void resetUnirestConfig() {
        Unirest.config().reset(); // Unirest 설정을 초기화
        Unirest.config().socketTimeout(5000).connectTimeout(5000); // 타임아웃 설정: 연결, 읽기 타임아웃을 5초로 설정
    }


    // List Sites 장소목록(모든 장소를 반환)
    public List<ListSiteDTO> listSites() throws UnirestException {
        resetUnirestConfig();
        try {
            HttpResponse<String> response = Unirest.post("https://djg.atlyst.metrocount.com/api/list_sites/")
                    .header("APIKEY", "Your API KEY")
                    .body("")
                    .asString();
            logger.debug(String.valueOf(response.getBody()));
            logger.debug(String.valueOf(response.getHeaders()));
            // 헤더값 로깅 (info, debug 두가지 모드)
            logService.listSiteResponseHeaders(response);
            // 응답 데이터 body 반환
            if (response.getStatus() == 200) {
                List<ListSiteDTO> sitesBody = new ObjectMapper().readValue(response.getBody(), new TypeReference<List<ListSiteDTO>>() {
                });
                sitesBody.forEach(site -> {
                    cacheSite(site.getSite_id());
                    logger.info("response list_sites Data: {} ", site);
                });
                return sitesBody;
            } else {
                logger.warn("Failed to fetch sites: HTTP {}", response.getStatus());
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing the sites list response", e);
            throw new RuntimeException("JSON parsing error", e);
        } catch (UnirestException e) {
            logger.error("Error fetching site data", e);
            throw e;
        }
    }


    //Individual Vehicles 개별 차량(특정 장소에 대한 개별 차량기록)
    //Input Parameters >>
    //site_id:          INTEGER  대상 장소 ID
    //start_timestamp:  String   ISO-8601 표준의 타임스탬프 형식(예: "2019-07-01T00:00")의 YYYY-MM-DDTHH:MM입니다. 쿼리는 시작_타임스탬프보다 크고 같지 않은 차량을 반환합니다.
    //limit:            INTEGER  쿼리에서 반환되는 최대 차량 수입니다. 기본값은 10,000이며 1에서 100,000 사이여야 합니다.
    public List<IndividualVehiclesDTO> individualVehicles(Integer siteId) throws UnirestException {
        resetUnirestConfig();
        //이전 차량 지나간 시간
        Timestamp lastProcessedTime = VehicleUtils.LastVehiclePassTimeManager.getLastVehiclePassTime(siteId);
        String startTime = formatStartTime(lastProcessedTime);
        //요청 파라미터 생성
        String VehiclesBody = buildRequestBody(siteId, startTime, limit);

        try {
            HttpResponse<String> response = Unirest.post("https://djg.atlyst.metrocount.com/api/individual_vehicles/")
                    .header("APIKEY", "Your API KEY")
                    .body(VehiclesBody) //요청 파라미터 추가
                    .asString();
            logger.debug(String.valueOf(response.getBody()));
            logger.debug(String.valueOf(response.getHeaders()));
            //헤더 로그 로깅
            logService.individualVehiclesResponseHeaders(response);
            // 응답 데이터 body 반환
            if (response.getStatus() == 200) {
                List<IndividualVehiclesDTO> vehicles = new ObjectMapper().readValue(response.getBody(), new TypeReference<List<IndividualVehiclesDTO>>() {
                });
                vehicles.forEach(vehicle -> vehicle.setSiteId(siteId));
                logger.info("Individual vehicles Data: {}", vehicles);
                return vehicles;
            } else {
                logger.warn("Failed to fetch sites: HTTP {}", response.getStatus());
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing the individual vehicles response", e);
            throw new RuntimeException("JSON parsing error", e);
        } catch (UnirestException e) {
            logger.error("API request failed", e);
            throw e;
        }
    }

    // 요청 파라미터 생성
    private String buildRequestBody(Integer siteId, String startTime, int limit) {
        return String.format("{\"site_id\":%d,\"start_timestamp\":\"%s\",\"limit\":%d}", siteId, startTime, limit);
    }

    // 시작 시간 포멧
    private String formatStartTime(Timestamp timestamp) {
        if (timestamp != null) {
            return timestamp.toString().replace(" ", "T");
        } else {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    //individualVehicles api 호출할때 사용할 site_id 관리
    public void cacheSite(Integer siteId) {
        siteCache.add(siteId); // Only store site_id
        logger.info("Caching site ID: {}", siteId); // Log the caching of each site ID
    }

    // Method to check if the cache is empty
    public boolean isCacheEmpty() {
        return siteCache.isEmpty();
    }

    // Method to access the site cache safely
    public Set<Integer> getSiteCache() {
        return Collections.unmodifiableSet(siteCache); // Return an unmodifiable view of the set
    }
}
