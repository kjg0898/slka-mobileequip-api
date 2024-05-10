package org.neighbor21.slkaMobileEquipApi.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.util
 * fileName       : VehicleUtils.java
 * author         : kjg08
 * date           : 24. 4. 23.
 * description    : 여러가지 유틸
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 23.        kjg08           최초 생성
 */
public class VehicleUtils {

    /**
     * 두 차량의 Timestamp를 받아 차량 간 간격(초)를 계산합니다.
     *
     * @param currentTimestamp  현재 차량의 타임스탬프
     * @param previousTimestamp 이전 차량의 타임스탬프
     * @return 차량 간 간격(초)
     */
    public static int calculateHeadway(Timestamp currentTimestamp, Timestamp previousTimestamp) {
        if (previousTimestamp == null) {
            return 0;  // 첫 번째 차량인 경우 간격은 0으로 설정
        }
        long headwayMilliseconds = currentTimestamp.getTime() - previousTimestamp.getTime();
        return (int) (headwayMilliseconds / 1000);  // 밀리초를 초로 변환
    }

    /**
     * Individual Vehicles 개별 차량(특정 장소에 대한 개별 차량기록) api 를 5분마다 호출 할 시에  이전에 이미 호출한 데이터를 호출하지 않기 위해
     * 그리고 이미 지나간 차량과 현재 통과하는 차량의 시간차이(초) 값을 구하기 위해
     * 서버가 재 시작한 경우에도 이어지기 위해서 마지막 시간을 파일에 저장하고, 필요할때 불러오는 util
     *
     */
    public static class LastVehiclePassTimeManager {
        private static final Logger logger = LoggerFactory.getLogger(LastVehiclePassTimeManager.class);
        private static final String LAST_PROCESSED_FILENAME = "last_vehicle_pass_time.txt";
        private static final Map<Integer, Timestamp> lastVehiclePassTimeMap = new HashMap<>();

        public static Timestamp getLastVehiclePassTime(Integer siteId) {
            return Optional.ofNullable(lastVehiclePassTimeMap.get(siteId))
                    .orElseGet(() -> {
                        logger.info("No existing timestamp found for siteId {}. Returning current time.", siteId);
                        return new Timestamp(System.currentTimeMillis());
                    });
        }

        public void loadLastVehiclePassTimes() {
            Path path = Paths.get(LAST_PROCESSED_FILENAME);
            try {
                if (!Files.exists(path)) {
                    logger.info("No existing file found. A new file will be created.");
                    saveLastVehiclePassTimes();  // Create the file if it doesn't exist.
                }

                List<String> lines = Files.readAllLines(path);
                lines.forEach(line -> {
                    String[] parts = line.split(",");
                    Integer siteId = Integer.parseInt(parts[0]);
                    Timestamp timestamp = Timestamp.valueOf(parts[1]);
                    lastVehiclePassTimeMap.put(siteId, timestamp);
                });
            } catch (IOException e) {
                logger.error("Failed to load last processed times", e);
            }
        }

        public void saveLastVehiclePassTimes() {
            List<String> lines = lastVehiclePassTimeMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "," + entry.getValue().toString())
                    .collect(Collectors.toList());
            try {
                Files.write(Paths.get(LAST_PROCESSED_FILENAME), lines);
            } catch (IOException e) {
                logger.error("Failed to save last processed times", e);
            }
        }
    }
}
