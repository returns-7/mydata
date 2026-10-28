package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataFundDTO;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MydataFundMapperTest {

    private static PooledDataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;

    private SqlSession sqlSession;
    private MydataFundMapper mydataFundMapper;

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
        mydataFundMapper = sqlSession.getMapper(MydataFundMapper.class);
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
    @DisplayName("존재하는 fundCode로 조회하면 해당 펀드 정보를 반환한다")
    void selectByFundCodeReturnsFundWhenFundCodeExists() throws SQLException {
        insertFund("448630", "TIGER 미국배당다우존스", 72.50, LocalDate.of(2023, 5, 10));

        Optional<MydataFundDTO> result = mydataFundMapper.selectByFundCode("448630");

        assertThat(result).isPresent();
        assertThat(result.get().getFundCode()).isEqualTo("448630");
        assertThat(result.get().getFundName()).isEqualTo("TIGER 미국배당다우존스");
        assertThat(result.get().getForeignStockRatio()).isEqualByComparingTo(BigDecimal.valueOf(72.50));
        assertThat(result.get().getInceptionDate()).isEqualTo(LocalDate.of(2023, 5, 10));
    }

    @Test
    @DisplayName("존재하지 않는 fundCode로 조회하면 빈 값을 반환한다")
    void selectByFundCodeReturnsEmptyWhenFundCodeNotExists() {
        Optional<MydataFundDTO> result = mydataFundMapper.selectByFundCode("999999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 fundCode로 여러 건이 있어도 요청한 fundCode의 데이터만 조회한다")
    void selectByFundCodeReturnsOnlyMatchingFund() throws SQLException {
        insertFund("069500", "KODEX 200", 5.00, LocalDate.of(2020, 1, 1));
        insertFund("381170", "TIGER 미국테크TOP10", 88.00, LocalDate.of(2021, 6, 1));

        Optional<MydataFundDTO> result = mydataFundMapper.selectByFundCode("381170");

        assertThat(result).isPresent();
        assertThat(result.get().getFundCode()).isEqualTo("381170");
        assertThat(result.get().getFundName()).isEqualTo("TIGER 미국테크TOP10");
    }

    private void resetSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("""
                    CREATE TABLE mydata_fund (
                        mydata_fund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        fund_code VARCHAR(12) NOT NULL UNIQUE,
                        fund_name VARCHAR(100) NOT NULL,
                        foreign_stock_ratio DECIMAL(5, 2),
                        inception_date DATE
                    )
                    """);
        }
    }

    private void insertFund(String fundCode, String fundName, Double foreignStockRatio, LocalDate inceptionDate) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO mydata_fund (fund_code, fund_name, foreign_stock_ratio, inception_date) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, fundCode);
            statement.setString(2, fundName);
            statement.setObject(3, foreignStockRatio);
            statement.setObject(4, inceptionDate);
            statement.executeUpdate();
        }
    }
}