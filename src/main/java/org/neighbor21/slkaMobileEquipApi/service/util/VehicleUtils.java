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
 * description    : 여러 가지 유틸리티 클래스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 23.        kjg08           최초 생성
 * 24. 5. 17.        user            주석 추가 및 description 갱신
 */
public class VehicleUtils {

    /**
     * 두 차량의 Timestamp를 받아 차량 간 간격(초)을 계산합니다.
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
     * Individual Vehicles 개별 차량(특정 장소에 대한 개별 차량 기록) API를 5분마다 호출할 때
     * 이전에 이미 호출한 데이터를 다시 호출하지 않기 위해,
     * 그리고 이미 지나간 차량과 현재 통과하는 차량의 시간 차이(초) 값을 구하기 위해,
     * 서버가 재시작한 경우에도 이어지기 위해서 마지막 시간을 파일에 저장하고, 필요할 때 불러오는 유틸리티 클래스
     */
    public static class LastVehiclePassTimeManager {
        private static final Logger logger = LoggerFactory.getLogger(LastVehiclePassTimeManager.class);
        private static final String LAST_PROCESSED_FILENAME = "last_vehicle_pass_time.txt";
        private static final Map<Integer, Timestamp> lastVehiclePassTimeMap = new HashMap<>();

        /**
         * siteId에 대한 마지막 차량 통과 시간을 반환합니다.
         *
         * @param siteId 장소 ID
         * @return 마지막 차량 통과 시간
         */
        public static Timestamp getLastVehiclePassTime(Integer siteId) {
            return Optional.ofNullable(lastVehiclePassTimeMap.get(siteId)) // Map에서 siteId에 해당하는 값을 가져와 Optional로 감쌈
                    .orElseGet(() -> { // 만약 값이 없으면 (Optional이 비어있으면) 실행
                        logger.info("No existing timestamp found for siteId {}. Returning current time minus one hour.", siteId);
                        return new Timestamp(System.currentTimeMillis() - 3600000); // 현재 시간에서 1시간(3600000 밀리초) 뺀 값을 반환
                    });
        }

        /**
         * siteId에 대한 마지막 차량 통과 시간을 업데이트합니다.
         *
         * @param siteId   장소 ID
         * @param passTime 업데이트할 통과 시간
         */
        public static void updateLastVehiclePassTime(Integer siteId, Timestamp passTime) {
            lastVehiclePassTimeMap.put(siteId, passTime);
        }

        /**
         * 마지막 차량 통과 시간을 파일에서 로드합니다.
         */
        public void loadLastVehiclePassTimes() {
            Path path = Paths.get(LAST_PROCESSED_FILENAME);
            try {
                if (!Files.exists(path)) {
                    logger.info("No existing file found. A new file will be created.");
                    saveLastVehiclePassTimes();  // 파일이 존재하지 않으면 파일을 생성합니다.
                } else {
                    List<String> lines = Files.readAllLines(path);
                    lines.forEach(line -> {
                        String[] parts = line.split(",");
                        Integer siteId = Integer.parseInt(parts[0]);
                        Timestamp timestamp = Timestamp.valueOf(parts[1]);
                        lastVehiclePassTimeMap.put(siteId, timestamp);
                    });
                }
            } catch (IOException e) {
                logger.error("Failed to load last processed times", e);
            }
        }

        /**
         * 마지막 차량 통과 시간을 파일에 저장합니다.
         */
        public static void saveLastVehiclePassTimes() {
            List<String> lines = lastVehiclePassTimeMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "," + entry.getValue().toString())
                    .collect(Collectors.toList());
            try {
                Path path = Paths.get(LAST_PROCESSED_FILENAME);
                Files.write(path, lines);
                logger.info("Successfully saved last processed times to {}", LAST_PROCESSED_FILENAME);
            } catch (IOException e) {
                logger.error("Failed to save last processed times", e);
            }
        }
    }
}
