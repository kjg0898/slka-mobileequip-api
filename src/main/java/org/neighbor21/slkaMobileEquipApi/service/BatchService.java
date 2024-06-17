package org.neighbor21.slkaMobileEquipApi.service;

import io.github.resilience4j.retry.Retry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.neighbor21.slkaMobileEquipApi.config.Constants;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PASSEntity;
import org.neighbor21.slkaMobileEquipApi.entity.TL_MVMNEQ_PERIODEntity;
import org.neighbor21.slkaMobileEquipApi.entity.compositeKey.TL_MVMNEQ_PASS_IdEntity;
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
 * packageName    : org.neighbor21.slkaMobileEquipApi.service
 * fileName       : BatchService.java
 * author         : kjg08
 * date           : 24. 5. 21.
 * description    : 배치로 엔티티를 삽입하는 서비스를 제공하는 클래스.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 21.        kjg08           최초 생성
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

    @Autowired
    private Retry dbRetry;


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
     * @param entities        삽입할 엔티티 리스트
     * @param persistFunction 엔티티 삽입 함수
     * @param <T>             엔티티 타입
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
                        Retry.decorateRunnable(dbRetry, () -> {
                            for (T entity : batchList) {
                                try {
                                    persistFunction.accept(entity); // 엔티티 삽입 함수 호출
                                } catch (Exception e) {
                                    logger.error("Error persisting entity {}: {}", entity, e.getMessage(), e);
                                    throw e; // 외부 catch에서 처리하도록 다시 던짐
                                }
                            }
                            entityManager.flush(); // 변경 사항을 데이터베이스에 반영
                            entityManager.clear(); // 영속성 컨텍스트를 비움
                        }).run();
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
     * @param batchList       현재 배치 리스트
     * @param persistFunction 엔티티 삽입 함수
     * @param <T>             엔티티 타입
     */
    private <T> void handleBatchException(List<T> batchList, Consumer<T> persistFunction) {
        for (T entity : batchList) {
            try {
                persistFunction.accept(entity);
                entityManager.flush();
            } catch (Exception e) {
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
     * JDBC를 사용하여 배치로 TL_MVMNEQ_PERIOD 데이터를 삽입하는 메소드.
     *
     * @param periodEntities 삽입할 엔티티 리스트
     * @throws SQLException SQL 예외 발생 시
     */
    public void insertPeriodeBatch(List<TL_MVMNEQ_PERIODEntity> periodEntities) throws SQLException {
        String sql = "INSERT INTO srlk.tl_mvmneq_period (clct_dt, sqno, instllc_id, start_dt, end_dt) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (clct_dt, instllc_id, sqno) DO NOTHING";

        // 데이터베이스에 연결을 설정합니다.
        try (Connection connection = Retry.decorateCheckedSupplier(dbRetry, dataSource::getConnection).apply()) {
            connection.setAutoCommit(false); // 자동 커밋 비활성화

            // 배치 실행을 위한 SQL 문을 준비합니다.
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (TL_MVMNEQ_PERIODEntity entity : periodEntities) {
                    // 엔티티에서 PreparedStatement의 매개변수를 설정합니다.
                    statement.setTimestamp(1, entity.getId().getCollectionDatetime());
                    statement.setInt(2, entity.getId().getSequenceNo());
                    statement.setString(3, entity.getId().getInstllcId());
                    statement.setTimestamp(4, Timestamp.valueOf(entity.getStartTime()));
                    statement.setTimestamp(5, Timestamp.valueOf(entity.getEndTime()));
                    statement.addBatch(); // 배치에 문을 추가합니다.
                }
                // 배치 실행
                statement.executeBatch();
                connection.commit(); // 트랜잭션 커밋
            } catch (SQLException e) {
                connection.rollback(); // 오류가 발생하면 트랜잭션 롤백
                logBatchUpdateException((BatchUpdateException) e); // 예외 로깅
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Failed to execute period batch insert", e);
            throw e; // 예외를 다시 던져서 호출자에게 알림
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JDBC를 사용하여 배치로 데이터를 삽입하는 메소드.
     *
     * @param passEntities 삽입할 엔티티 리스트
     * @throws SQLException SQL 예외 발생 시
     */
    public void insertPassBatch(List<TL_MVMNEQ_PASSEntity> passEntities) throws SQLException {
        String sql = "INSERT INTO srlk.tl_mvmneq_pass (pass_dt, vhcl_drct, pass_lane, instllc_id, vhcl_speed, vhcl_len, vhcl_intv_ss, vhcl_clsf) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (pass_dt, vhcl_drct, pass_lane, instllc_id) DO NOTHING";

        try (Connection connection = Retry.decorateCheckedSupplier(dbRetry, dataSource::getConnection).apply()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (TL_MVMNEQ_PASSEntity entity : passEntities) {
                    TL_MVMNEQ_PASS_IdEntity id = entity.getId();
                    statement.setTimestamp(1, id.getPassTime());
                    statement.setString(2, id.getVehicleDirection());
                    statement.setInt(3, id.getPassLane());
                    statement.setString(4, id.getInstllcId());
                    statement.setBigDecimal(5, entity.getVehicleSpeed());
                    statement.setBigDecimal(6, entity.getVehicleLength());
                    statement.setInt(7, entity.getVehicleIntervalSeconds());
                    statement.setString(8, entity.getVehicleClass());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                logBatchUpdateException((BatchUpdateException) e);
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Failed to execute pass batch insert", e);
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
