package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MydataRiaAccountMapperTest {

    private static PooledDataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;

    private SqlSession sqlSession;
    private MydataRiaAccountMapper mydataRiaAccountMapper;

    @BeforeAll
    static void configureMyBatis() throws IOException {
        try (Reader reader = Resources.getResourceAsReader("mybatis-mydata-test-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        dataSource = (PooledDataSource) sqlSessionFactory
                .getConfiguration()
                .getEnvironment()
                .getDataSource();
    }

    @BeforeEach
    void setUpDatabase() throws SQLException {
        resetSchema();
        sqlSession = sqlSessionFactory.openSession(true);
        mydataRiaAccountMapper = sqlSession.getMapper(MydataRiaAccountMapper.class);
    }

    @AfterEach
    void closeSession() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource != null) {
            dataSource.forceCloseAll();
        }
    }

    @Test
    @DisplayName("동일 ciHash의 RIA 계좌를 모두 조회한다")
    void selectByCiHashReturnsAllAccountsForSameCiHash() {
        insertAccount("ci-1", "증권사A");
        insertAccount("ci-1", "증권사B");
        insertAccount("ci-2", "증권사C");

        List<MydataRiaAccountDTO> result = mydataRiaAccountMapper.selectByCiHash("ci-1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MydataRiaAccountDTO::getBrokerName)
                .containsExactlyInAnyOrder("증권사A", "증권사B");
    }

    @Test
    @DisplayName("다른 ciHash의 계좌는 결과에 포함하지 않는다")
    void selectByCiHashExcludesAccountsForDifferentCiHash() {
        insertAccount("ci-1", "증권사A");
        insertAccount("ci-2", "증권사B");

        List<MydataRiaAccountDTO> result = mydataRiaAccountMapper.selectByCiHash("ci-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrokerName()).isEqualTo("증권사A");
    }

    @Test
    @DisplayName("해당 ciHash의 계좌가 없으면 빈 리스트를 반환한다")
    void selectByCiHashReturnsEmptyListWhenNoAccountsExist() {
        List<MydataRiaAccountDTO> result = mydataRiaAccountMapper.selectByCiHash("no-account");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("계좌를 등록하면 저장된 값 그대로 조회된다")
    void insertAccountPersistsAllFields() {
        MydataRiaAccountDTO tradeDTO = MydataRiaAccountDTO.builder()
                .ciHash("ci-1")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.valueOf(5_000_000))
                .build();

        mydataRiaAccountMapper.insertAccount(tradeDTO);

        List<MydataRiaAccountDTO> result = mydataRiaAccountMapper.selectByCiHash("ci-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRiaLimit()).isEqualByComparingTo(BigDecimal.valueOf(30_000_000));
        assertThat(result.get(0).getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
    }

    @Test
    @DisplayName("동일 계좌를 upsert하면 최신 한도만 갱신하고 누적매도금액은 보존한다")
    void upsertAccountUpdatesOnlyLimitForExistingAccount() {
        MydataRiaAccountDTO original = MydataRiaAccountDTO.builder()
                .ciHash("ci-1")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.valueOf(5_000_000))
                .build();
        mydataRiaAccountMapper.insertAccount(original);

        MydataRiaAccountDTO update = MydataRiaAccountDTO.builder()
                .ciHash("ci-1")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(40_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        mydataRiaAccountMapper.upsertAccount(update);

        MydataRiaAccountDTO result = mydataRiaAccountMapper.selectByCiHash("ci-1").get(0);
        assertThat(result.getRiaLimit()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        assertThat(result.getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
    }

    @Test
    @DisplayName("동시에 같은 계좌를 upsert해도 한 행만 저장되고 누적매도금액은 0으로 생성된다")
    void concurrentUpsertCreatesSingleAccount() throws Exception {
        MydataRiaAccountDTO firstRequest = account("ci-1", "증권사A", 30_000_000);
        MydataRiaAccountDTO secondRequest = account("ci-1", "증권사A", 40_000_000);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> upsertAfterStart(firstRequest, ready, start));
            Future<?> second = executor.submit(() -> upsertAfterStart(secondRequest, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        List<MydataRiaAccountDTO> result = mydataRiaAccountMapper.selectByCiHash("ci-1");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRiaLimit().intValueExact())
                .isIn(30_000_000, 40_000_000);
        assertThat(result.get(0).getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ciHash와 증권사명이 모두 일치하는 계좌를 조회한다")
    void selectByCiHashAndBrokerNameReturnsMatchingAccount() {
        insertAccount("ci-1", "증권사A");
        insertAccount("ci-1", "증권사B");

        MydataRiaAccountDTO condition = MydataRiaAccountDTO.builder()
                .ciHash("ci-1")
                .brokerName("증권사B")
                .build();

        Optional<MydataRiaAccountDTO> result =
                mydataRiaAccountMapper.selectByCiHashAndBrokerName(condition);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getBrokerName()).isEqualTo("증권사B");
    }

    private void resetSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("""
                    CREATE TABLE mydata_ria_account (
                        mydata_account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        ci_hash VARCHAR(64) NOT NULL,
                        broker_name VARCHAR(50) NOT NULL,
                        ria_limit DECIMAL(15, 2) NOT NULL,
                        ria_cumulative_sell DECIMAL(15, 2) NOT NULL,
                        UNIQUE (ci_hash, broker_name)
                    )
                    """);
        }
    }

    private void insertAccount(String ciHash, String brokerName) {
        MydataRiaAccountDTO accountDTO = MydataRiaAccountDTO.builder()
                .ciHash(ciHash)
                .brokerName(brokerName)
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        mydataRiaAccountMapper.insertAccount(accountDTO);
    }

    private void upsertAfterStart(
            MydataRiaAccountDTO accountDTO,
            CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 upsert 시작 대기 시간 초과");
            }
            upsertWithRetry(accountDTO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 upsert가 중단되었습니다.", e);
        }
    }

    private void upsertWithRetry(MydataRiaAccountDTO accountDTO) {
        try {
            upsertOnce(accountDTO);
        } catch (PersistenceException e) {
            // H2의 MERGE INTO는 Postgres의 ON CONFLICT DO UPDATE와 달리 동시 upsert에
            // 원자성을 보장하지 않아 유니크 제약 위반이 날 수 있다. 운영에서는
            // MydataRiaAccountUpsertExecutor가 같은 이유로 새 트랜잭션에서 1회 재시도하므로,
            // 이 테스트도 그 재시도 계약을 그대로 반영한다.
            upsertOnce(accountDTO);
        }
    }

    private void upsertOnce(MydataRiaAccountDTO accountDTO) {
        try (SqlSession concurrentSession = sqlSessionFactory.openSession(true)) {
            concurrentSession.getMapper(MydataRiaAccountMapper.class).upsertAccount(accountDTO);
        }
    }

    private MydataRiaAccountDTO account(String ciHash, String brokerName, long riaLimit) {
        return MydataRiaAccountDTO.builder()
                .ciHash(ciHash)
                .brokerName(brokerName)
                .riaLimit(BigDecimal.valueOf(riaLimit))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
    }
}
