package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
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

    @Test
    @DisplayName("계좌 ID, ciHash, 증권사명이 일치하면 한도만 수정한다")
    void updateAccountUpdatesOnlyLimitOfMatchingAccount() {
        insertAccount("ci-1", "증권사A");
        MydataRiaAccountDTO savedAccount = mydataRiaAccountMapper.selectByCiHash("ci-1").get(0);
        savedAccount.setRiaLimit(BigDecimal.valueOf(40_000_000));
        savedAccount.setRiaCumulativeSell(BigDecimal.valueOf(10_000_000));

        mydataRiaAccountMapper.updateAccount(savedAccount);

        MydataRiaAccountDTO result = mydataRiaAccountMapper.selectByCiHash("ci-1").get(0);
        assertThat(result.getRiaLimit()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        assertThat(result.getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.ZERO);
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
}
