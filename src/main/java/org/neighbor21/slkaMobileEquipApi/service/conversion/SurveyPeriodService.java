package org.neighbor21.slkaMobileEquipApi.service.conversion;

import org.neighbor21.slkaMobileEquipApi.dto.listSite.ListSiteDTO;
import org.neighbor21.slkaMobileEquipApi.dto.listSite.SurveyPeriodDTO;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PERIOD_IdEntity;
import org.neighbor21.slkaMobileEquipApi.jpaRepository.TL_MVMNEQ_PERIODRepository;
import org.neighbor21.slkaMobileEquipApi.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 조사 기간 데이터를 처리하고 저장하는 서비스 클래스.
 */
@Service
public class SurveyPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyPeriodService.class);

    @Autowired
    private TL_MVMNEQ_PERIODRepository tlMvmneqPeriodRepository;

    @Autowired
    private BatchService batchService;


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

        // 엔티티 생성 및 순번 미리 할당한다. 왜냐하면 같은 설치위치 id 에 연속으로 starttime 이 들어오게 된다면 순번을 계산할때에 중복이 나거나 불확실한값이 들어갈수 있는데,
        // 이를 방지하려 매번 로직 안에서 조회를 하게 된다면 엄청난 성능의 저하가 일어나기 때문에 각각 처리로 들어가기 전에 한번에 미리 할당하는 과정
        // 엔티티 생성 및 순번 미리 할당
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
                    logger.error("Failed to parse date: start time - {}, end time - {}", startTimeStr, endTimeStr, e);
                    throw e;
                }
            }
        }

        // 엔티티 리스트를 배치로 삽입
        long dbStartTime = System.currentTimeMillis();
        try {
            /*batchService.batchInsertWithRetry(periodEntities, this::insertEntityInOrder);*/
            batchService.insertBatch(periodEntities); // JDBC를 사용한 배치 삽입
        } catch (Exception e) {
            logger.error("TL_MVMNEQ_PERIOD 배치 삽입 실패", e);
        }
        long dbEndTime = System.currentTimeMillis();
        logger.info("TL_MVMNEQ_PERIOD 배치 삽입 작업에 걸린 시간: {} ms", (dbEndTime - dbStartTime));
    }


    /**
     * 설치위치 ID에 대한 최대 순번 값을 조회한 결과를 Map으로 변환하는 메소드.
     *
     * @param instllcIds 설치위치 ID 리스트
     * @return 최대 순번 값이 포함된 Map
     */
    public Map<String, Integer> findMaxSequenceNoByInstllcIdsWithLogging(List<String> instllcIds) {
        Map<String, Integer> maxSequenceMap = new HashMap<>();

        int maxSequenceBatchSize = 4000; // 배치 크기를 설정
        for (int i = 0; i < instllcIds.size(); i += maxSequenceBatchSize) {
            int end = Math.min(i + maxSequenceBatchSize, instllcIds.size());
            List<String> batch = instllcIds.subList(i, end);
            List<Object[]> results = tlMvmneqPeriodRepository.findMaxSequenceNoByInstllcIds(batch);

            for (Object[] result : results) {
                maxSequenceMap.put((String) result[0], ((Number) result[1]).intValue());
            }
        }

        return maxSequenceMap;
    }
}