package org.neighbor21.slkaMobileEquipApi.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    public BatchService(RetryConfig retryConfig, TransactionTemplate transactionTemplate) {
        this.retryConfig = retryConfig;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 엔티티 리스트를 배치로 삽입하는 메서드로, 실패 시 재시도 기능을 포함한다.
     * 각 엔티티를 지속하고, 주기적으로 EntityManager를 플러시 및 클리어하여 메모리 사용을 최적화한다.
     *
     * @param entities 삽입할 엔티티 리스트
     */
    @Transactional
    public void batchInsertWithRetry(List<?> entities) {
        int batchSize = Constants.DEFAULT_BATCH_SIZE; // 배치 크기 설정
        Retry retry = Retry.of("batchInsert", retryConfig); // 재시도 설정
        AtomicInteger index = new AtomicInteger(0); // 처리된 엔티티 수를 추적하는 AtomicInteger

        entities.forEach(entity -> {
            Supplier<Boolean> insertSupplier = Retry.decorateSupplier(retry, () -> {
                int currentIndex = index.incrementAndGet(); // 현재 인덱스 증가
                return transactionTemplate.execute(status -> {
                    try {
                        Object primaryKey = getPrimaryKey(entity); // 엔티티의 기본 키를 가져옴
                        if (primaryKey != null && (entityManager.contains(entity) || entityManager.find(entity.getClass(), primaryKey) != null)) {
                            entityManager.merge(entity); // 엔티티 병합
                        } else {
                            entityManager.persist(entity); // 엔티티 삽입
                        }
                        if (currentIndex % batchSize == 0 || currentIndex == entities.size()) {
                            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
                            entityManager.clear(); // 영속성 컨텍스트를 비움
                        }
                        return true;
                    } catch (Exception e) {
                        logger.warn("Attempt to insert entity at index {} failed: {}", currentIndex, e.getMessage(), e);
                        status.setRollbackOnly(); // 트랜잭션 롤백
                        throw e;
                    }
                });
            });
            try {
                insertSupplier.get(); // 재시도 로직 실행
            } catch (Exception e) {
                logger.error("Failed to insert entity at index {} after retries", index.get(), e);
            }
        });

        transactionTemplate.execute(status -> {
            entityManager.flush(); // 남은 변경 사항을 데이터베이스에 반영
            entityManager.clear(); // 영속성 컨텍스트를 비움
            return null;
        });
    }

    /**
     * 엔티티의 기본 키를 가져오는 메소드.
     *
     * @param entity 기본 키를 가져올 엔티티
     * @return 기본 키 객체
     */
    private Object getPrimaryKey(Object entity) {
        try {
            for (Field field : entity.getClass().getDeclaredFields()) { // 엔티티의 모든 필드를 반복
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) { // 기본 키 필드를 찾음
                    field.setAccessible(true); // 필드 접근 가능 설정
                    return field.get(entity); // 필드 값 반환
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("Failed to access primary key field: {}", e.getMessage(), e);
        }
        return null;
    }
}