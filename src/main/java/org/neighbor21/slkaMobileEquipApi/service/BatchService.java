package org.neighbor21.slkaMobileEquipApi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service
 * fileName       : BatchService.java
 * author         : kjg08
 * date           : 24. 5. 9.
 * description    : 배치 처리를 위한 서비스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 9.        kjg08           최초 생성
 */
@Service
public class BatchService {
    private static final Logger logger = LoggerFactory.getLogger(BatchService.class);

    //배치처리
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 일괄 삽입을 시도하고, 실패할 경우 재시도하는 메서드.
     *
     * @param entities 삽입할 엔티티 리스트
     */
    @Transactional
    public void batchInsertWithRetry(List<?> entities) {
        // 최대 재시도 횟수
        int maxRetries = 2;
        int batchSize = 50;

        for (int i = 0; i < entities.size(); i++) {
            boolean success = false;
            int retryCount = 0;

            while (!success && retryCount < maxRetries) {
                try {
                    entityManager.persist(entities.get(i));
                    if ((i + 1) % batchSize == 0 || i == entities.size() - 1) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                    success = true;  // 성공하면 루프 탈출
                } catch (Exception e) {
                    retryCount++;
                    logger.error("Retry " + retryCount + " for entity at index " + i + " failed: " + e.getMessage(), e);
                    entityManager.clear();  // 실패 시 entityManager를 clear 하고 다시 시도
                    if (retryCount < maxRetries) {
                        backOff(retryCount);  // 백오프 로직으로 대체
                    } else {
                        logger.error("Failed to insert entity after " + maxRetries + " retries, at index: " + i);
                        // 실패 처리 로직 추가 가능
                    }
                }
            }
        }
    }
    /**
     * 백오프 로직: 재시도 간의 대기 시간을 지연시키는 메서드.
     *
     * @param retryCount 현재 재시도 횟수
     */
    private void backOff(int retryCount) {
        try {
            long backOffTime = (long) Math.pow(2, retryCount) * 100L; // 지수 백오프
            Thread.sleep(backOffTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted during backoff delay", ie);
        }
    }
}
