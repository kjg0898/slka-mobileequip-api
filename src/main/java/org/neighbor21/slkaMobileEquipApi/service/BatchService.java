package org.neighbor21.slkaMobileEquipApi.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.service
 * fileName       : BatchService.java
 * author         : kjg08
 * date           : 24. 5. 9.
 * description    : Hibernate Batch Processing을 통해 엔티티 리스트를 배치로 삽입하는 서비스 클래스. 실패 시 재시도 기능을 포함하며, Spring Batch가 아닌 Hibernate를 이용한 배치 처리 방식이다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 9.        kjg08           최초 생성
 * 24. 5. 17.        kjg08            주석 추가 및 description 갱신
 */
@Service
public class BatchService {
    private static final Logger logger = LoggerFactory.getLogger(BatchService.class);
    private final RetryConfig retryConfig;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * BatchService 생성자.
     *
     * @param retryConfig 재시도 구성 설정
     */
    @Autowired
    public BatchService(RetryConfig retryConfig, PlatformTransactionManager transactionManager) {
        this.retryConfig = retryConfig;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 엔티티 리스트를 배치로 삽입하는 메소드. Resilience4j를 사용하여 재시도 로직을 구현함.
     * 각 엔티티를 지속하고, 주기적으로 entityManager 플러시 및 클리어하여 메모리 사용을 최적화한다.
     *
     * @param entities 삽입할 엔티티 리스트
     */
    @Transactional
    public <T> void batchInsertWithRetry(List<T> entities, BatchInsertFunction<T> insertFunction) {

        int batchSize = Constants.DEFAULT_BATCH_SIZE; // 배치 크기 설정
        Retry retry = Retry.of("batchInsert", retryConfig); // 재시도 설정
        int totalRecords = entities.size();


        transactionTemplate.executeWithoutResult(status -> { // 이 블록 내의 코드는 트랜잭션 내에서 실행됩니다.
            // entities 리스트를 batchSize 크기만큼씩 나누어 처리하는 루프
            for (int i = 0; i < totalRecords; i += batchSize) {
                // 현재 배치의 끝 인덱스를 계산
                int end = Math.min(i + batchSize, totalRecords);
                // 현재 배치 리스트를 추출
                List<T> batchList = entities.subList(i, end);

                if (!batchList.isEmpty()) {
                    // Retry.decorateSupplier를 사용하여 재시도 로직을 감싼 Supplier 생성 함수형 인터페이스이므로, 람다 표현식이나 메서드 참조를 사용하여 간단하게 구현 지연 계산: Supplier는 계산 결과를 즉시 반환하지 않고, 필요할 때 계산하여 반환 파라미터화된 객체 생성: Supplier를 사용하여 특정 조건이나 상황에 따라 객체를 생성
                    Supplier<Boolean> insertSupplier = Retry.decorateSupplier(retry, () -> {
                        try {
                            for (T entity : batchList) {
                                insertFunction.insert(entityManager, entity); // 엔티티 삽입 함수 호출
                            }
                            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
                            entityManager.clear(); // 영속성 컨텍스트를 비움

                            return true;
                        } catch (Exception e) {
                            logger.warn("Batch insert attempt failed: {}", e.getMessage(), e);
                            try {
                                throw e;
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });

                    try {
                        insertSupplier.get();
                    } catch (Exception e) {
                        logger.error("Failed to insert batch at index {} to {}", i, end, e);
                    }
                }
            }
        });


    }

    @FunctionalInterface
    public interface BatchInsertFunction<T> {
        void insert(EntityManager entityManager, T entity) throws Exception;
    }
}