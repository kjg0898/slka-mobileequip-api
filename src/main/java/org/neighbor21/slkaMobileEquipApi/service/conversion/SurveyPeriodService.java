package org.neighbor21.slkaMobileEquipApi.service.conversion;

import io.github.resilience4j.retry.Retry;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.SurveyPeriodDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;
import org.neighbor21.slkaMobileEquipApi.jpaRepository.TL_MVMNEQ_PERIODRepository;
import org.neighbor21.slkaMobileEquipApi.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service.conversion
 * fileName       : SurveyPeriodService.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    : 조사 기간 데이터를 처리하고 저장하는 서비스 클래스.
 * 이 클래스는 개별 차량 통과 데이터를 받아와서 데이터베이스에 저장하는 작업을 수행한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
 */
@Service
public class SurveyPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyPeriodService.class);

    @Autowired
    private TL_MVMNEQ_PERIODRepository tlMvmneqPeriodRepository;

    @Autowired
    private BatchService batchService;

    @Autowired
    @Qualifier("dbRetry")
    private Retry retry; // Retry 객체 주입

    /**
     * 조사 기간 정보를 받아와서 데이터베이스에 저장하는 메소드.
     *
     * @param periods List<ListSiteDTO> 조사 기간 정보 리스트
     */
    @Transactional
    public void saveSurveyPeriods(List<ListSiteDTO> periods) {
        List<TL_MVMNEQ_PERIODEntity> periodEntities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 설치위치id 에 대한 최대순번을 한번에 가져오기 위해 먼저 모든 설치 위치 ID를 수집
        List<String> instllcIds = periods.stream()
                .map(period -> period.getSite_id().toString())
                .distinct()
                .collect(Collectors.toList());

        // 모든 설치 위치 ID에 대한 최대 순번을 한 번에 조회
        Map<String, Integer> maxSequenceMap = findMaxSequenceNoByInstllcIdsWithLogging(instllcIds);

        // 엔티티 생성 및 순번 미리 할당. 같은 설치위치 id에 연속으로 starttime이 들어오면 순번 계산 시 중복 또는 불확실한 값 발생 방지
        for (ListSiteDTO period : periods) {
            List<SurveyPeriodDTO> surveyPeriods = period.getSurvey_periods().stream()
                    .sorted(Comparator.comparing(SurveyPeriodDTO::getStart_time))
                    .toList();

            String instllcId = period.getSite_id().toString();
            Integer currentMaxSequence = maxSequenceMap.getOrDefault(instllcId, 0);

            // 각 조사 기간을 엔티티로 변환하여 리스트에 추가
            for (int i = 0; i < surveyPeriods.size(); i++) {
                SurveyPeriodDTO periodDTO = surveyPeriods.get(i);
                TL_MVMNEQ_PERIODEntity periodEntity = new TL_MVMNEQ_PERIODEntity();
                TL_MVMNEQ_PERIOD_IdEntity periodIdEntity = new TL_MVMNEQ_PERIOD_IdEntity();

                periodIdEntity.setCollectionDatetime(new Timestamp(System.currentTimeMillis()));
                periodIdEntity.setSequenceNo(currentMaxSequence + i + 1);
                periodIdEntity.setInstllcId(instllcId);

                String startTimeStr = periodDTO.getStart_time().replace("T", " ");
                String endTimeStr = periodDTO.getEnd_time().replace("T", " ");

                try {
                    LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
                    LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

                    periodEntity.setId(periodIdEntity);
                    periodEntity.setStartTime(Timestamp.valueOf(startTime).toLocalDateTime());
                    periodEntity.setEndTime(Timestamp.valueOf(endTime).toLocalDateTime());

                    periodEntities.add(periodEntity);
                } catch (DateTimeParseException e) {
                    logger.error("날짜를 파싱하지 못했습니다: 시작 시간 - {}, 종료 시간 - {}", startTimeStr, endTimeStr, e);
                    // 날짜 파싱 예외 발생 시 예외를 던져 호출자가 처리할 수 있도록 합니다.
                    throw e;
                }
            }
        }

        // 엔티티 리스트를 배치로 삽입
        long dbStartTime = System.currentTimeMillis();
        try {
            // JDBC를 사용한 배치 삽입
            Retry.decorateRunnable(retry, () -> {
                try {
                    batchService.insertPeriodeBatch(periodEntities);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).run();
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PERIOD 배치 삽입 실패", e);
            // 예외 발생 시 추가적인 예외 처리를 수행할 수 있습니다. 예: 알림 발송, 재시도 로직 등
        }
        long dbEndTime = System.currentTimeMillis();
        logger.info("TL_MVMNEQ_PERIOD 배치 삽입 작업에 걸린 총 시간: {} ms", (dbEndTime - dbStartTime));
    }

    /**
     * 설치위치 ID에 대한 최대 순번 값을 조회한 결과를 Map으로 변환하는 메소드.
     *
     * @param instllcIds 설치위치 ID 리스트
     * @return 최대 순번 값이 포함된 Map
     */
    public Map<String, Integer> findMaxSequenceNoByInstllcIdsWithLogging(List<String> instllcIds) {
        Map<String, Integer> maxSequenceMap = new HashMap<>();

        int batchSize = Constants.DEFAULT_BATCH_SIZE; // 배치 크기를 설정
        for (int i = 0; i < instllcIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, instllcIds.size());
            List<String> batch = instllcIds.subList(i, end);
            List<Object[]> results = tlMvmneqPeriodRepository.findMaxSequenceNoByInstllcIds(batch);

            for (Object[] result : results) {
                maxSequenceMap.put((String) result[0], ((Number) result[1]).intValue());
            }
        }

        return maxSequenceMap;
    }
}
