package org.neighbor21.slkaMobileEquipApi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * 배치로 엔티티를 삽입하는 서비스를 제공하는 클래스.
 */
@Service
@Transactional
public class BatchService {
    private static final Logger logger = LoggerFactory.getLogger(BatchService.class);
    private final TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * BatchService 생성자.
     *
     * @param transactionManager 트랜잭션 매니저
     */
    @Autowired
    public BatchService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 엔티티 리스트를 배치로 삽입하는 메소드.
     * 각 엔티티를 지속하고, 주기적으로 EntityManager를 플러시 및 클리어하여 메모리 사용을 최적화한다.
     *
     * @param entities 삽입할 엔티티 리스트
     * @param persistFunction 엔티티 삽입 함수
     * @param <T> 엔티티 타입
     */
    public <T> void batchInsertWithRetry(List<T> entities, Consumer<T> persistFunction) {
        int batchSize = Constants.DEFAULT_BATCH_SIZE; // 배치 크기 설정
        int totalRecords = entities.size();
        final int[] lastLoggedProgress = {0}; // 배치 시작 시 진행률 초기화

        transactionTemplate.executeWithoutResult(status -> {

            // entities 리스트를 batchSize 크기만큼씩 나누어 처리하는 루프
            for (int i = 0; i < totalRecords; i += batchSize) {
                // 현재 배치의 끝 인덱스를 계산
                int end = Math.min(i + batchSize, totalRecords);
                // 현재 배치 리스트를 추출
                List<T> batchList = entities.subList(i, end);

                if (!batchList.isEmpty()) {
                    try {
                        for (T entity : batchList) {
                            try {
                                persistFunction.accept(entity); // 엔티티 삽입 함수 호출
                            } catch (Exception e) {
                                logger.error("Error persisting entity {}: {}", entity, e.getMessage(), e);
                                throw e; // Rethrow to handle in outer catch
                            }
                        }
                        entityManager.flush(); // 변경 사항을 데이터베이스에 반영
                        entityManager.clear(); // 영속성 컨텍스트를 비움
                        // 진행률 계산 및 로그 출력
                        int progress = (int) (((double) end / totalRecords) * 100);
                        if (progress >= lastLoggedProgress[0] + 10) {
                            logger.info("Batch insert progress: {}%", progress);
                            lastLoggedProgress[0] = progress;
                        }
                    } catch (Exception e) {
                        logger.error("Batch insert attempt failed at index {} to {}: {}", i, end, e.getMessage(), e);
                        handleBatchException(batchList, persistFunction);
                    }
                }
            }
        });
    }

    /**
     * 배치 삽입 중 예외가 발생했을 때 예외를 처리하는 메소드.
     *
     * @param batchList 현재 배치 리스트
     * @param persistFunction 엔티티 삽입 함수
     * @param <T> 엔티티 타입
     */
    private <T> void handleBatchException(List<T> batchList, Consumer<T> persistFunction) {
        for (T entity : batchList) {
            try {
                persistFunction.accept(entity);
                entityManager.flush();
            } catch (Exception e) {
                if (e instanceof BatchUpdateException) {
                    logBatchUpdateException((BatchUpdateException) e);
                }
                logger.error("Failed to insert entity {}: {}", entity, e.getMessage());
            } finally {
                entityManager.clear(); // 영속성 컨텍스트를 비움
            }
        }
    }

    /**
     * 배치 업데이트 예외를 로깅하는 메소드.
     *
     * @param bue BatchUpdateException 인스턴스
     */
    private void logBatchUpdateException(BatchUpdateException bue) {
        SQLException nextException = bue.getNextException();
        while (nextException != null) {
            logger.error("Next SQLException: SQLState: {}, ErrorCode: {}, Message: {}",
                    nextException.getSQLState(),
                    nextException.getErrorCode(),
                    nextException.getMessage());
            nextException = nextException.getNextException();
        }
    }

    /**
     * JDBC를 사용하여 배치로 데이터를 삽입하는 메소드.
     *
     * @param periodEntities 삽입할 엔티티 리스트
     * @throws SQLException SQL 예외 발생 시
     */
    public void insertBatch(List<TL_MVMNEQ_PERIODEntity> periodEntities) throws SQLException {
        String sql = "INSERT INTO srlk.tl_mvmneq_period (clct_dt, sqno, instllc_id, start_dt, end_dt) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (clct_dt, instllc_id, sqno) DO NOTHING";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false); // 자동 커밋 비활성화
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (TL_MVMNEQ_PERIODEntity entity : periodEntities) {
                    statement.setTimestamp(1, entity.getId().getCollectionDatetime());
                    statement.setInt(2, entity.getId().getSequenceNo());
                    statement.setString(3, entity.getId().getInstllcId());
                    statement.setTimestamp(4, Timestamp.valueOf(entity.getStartTime()));
                    statement.setTimestamp(5, Timestamp.valueOf(entity.getEndTime()));
                    statement.addBatch();
                }

                statement.executeBatch(); // 배치 실행
                connection.commit(); // 커밋
            } catch (SQLException e) {
                connection.rollback(); // 롤백
                throw e;
            }
        }
    }
}