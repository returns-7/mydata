package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataTradeDTO;
import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.type.StockType;
import com.app.mydata.domain.mydata.type.TradeType;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MydataTradeMapperTest {

    private static PooledDataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;

    private SqlSession sqlSession;
    private MydataTradeMapper mydataTradeMapper;

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
        mydataTradeMapper = sqlSession.getMapper(MydataTradeMapper.class);
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
    @DisplayName("동일 ciHash의 거래 내역을 거래일자 오름차순으로 조회한다")
    void selectByCiHashAndPeriodReturnsTradesOrderedByDate() {
        insertTrade("ci-1", LocalDate.of(2026, 3, 5));
        insertTrade("ci-1", LocalDate.of(2026, 1, 10));
        insertTrade("ci-2", LocalDate.of(2026, 2, 1));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(result.get(1).getTradeDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("fromDate 이전 거래는 결과에서 제외한다")
    void selectByCiHashAndPeriodExcludesTradesBeforeFromDate() {
        insertTrade("ci-1", LocalDate.of(2026, 1, 1));
        insertTrade("ci-1", LocalDate.of(2026, 6, 1));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .fromDate(LocalDate.of(2026, 3, 1))
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    @DisplayName("toDate 이후 거래는 결과에서 제외한다")
    void selectByCiHashAndPeriodExcludesTradesAfterToDate() {
        insertTrade("ci-1", LocalDate.of(2026, 1, 1));
        insertTrade("ci-1", LocalDate.of(2026, 6, 1));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .toDate(LocalDate.of(2026, 3, 1))
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("fromDate와 toDate가 모두 없으면 해당 ciHash의 전체 거래를 조회한다")
    void selectByCiHashAndPeriodReturnsAllTradesWhenDateRangeOmitted() {
        insertTrade("ci-1", LocalDate.of(2020, 1, 1));
        insertTrade("ci-1", LocalDate.of(2030, 1, 1));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("해당 ciHash의 거래가 없으면 빈 리스트를 반환한다")
    void selectByCiHashAndPeriodReturnsEmptyListWhenNoTradesExist() {
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("no-trades")
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("FUND 타입 거래는 fund_code가 저장/조회된다")
    void selectByCiHashAndPeriodReturnsFundCodeWhenStockTypeIsFund() {
        insertTrade("ci-1", LocalDate.of(2026, 3, 5), StockType.FUND, "KR5201234567");

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockType()).isEqualTo(StockType.FUND);
        assertThat(result.get(0).getFundCode()).isEqualTo("KR5201234567");
    }

    @Test
    @DisplayName("FOREIGN_STOCK/ETF/ETN 타입 거래는 ticker가 저장/조회된다")
    void selectByCiHashAndPeriodReturnsTickerWhenStockTypeIsNotFund() {
        insertTrade("ci-1", LocalDate.of(2026, 3, 5), StockType.FOREIGN_STOCK, null, "AAPL");

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("ci-1")
                .build();

        List<MydataTradeDTO> result = mydataTradeMapper.selectByCiHashAndPeriod(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockType()).isEqualTo(StockType.FOREIGN_STOCK);
        assertThat(result.get(0).getTicker()).isEqualTo("AAPL");
    }

    private void resetSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("""
                    CREATE TABLE mydata_trade (
                        trade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        ci_hash VARCHAR(64) NOT NULL,
                        broker_name VARCHAR(50) NOT NULL,
                        trade_type VARCHAR(20) NOT NULL,
                        stock_type VARCHAR(20) NOT NULL,
                        fund_code VARCHAR(12) NULL,
                        ticker VARCHAR(20) NULL,
                        qty DECIMAL(15, 2) NOT NULL,
                        trade_date DATE NOT NULL,
                        amount DECIMAL(15, 2) NOT NULL
                    )
                    """);
        }
    }

    private void insertTrade(String ciHash, LocalDate tradeDate) {
        insertTrade(ciHash, tradeDate, StockType.FOREIGN_STOCK, null, "AAPL");
    }

    private void insertTrade(String ciHash, LocalDate tradeDate, StockType stockType, String fundCode) {
        insertTrade(ciHash, tradeDate, stockType, fundCode, null);
    }

    private void insertTrade(String ciHash, LocalDate tradeDate, StockType stockType, String fundCode, String ticker) {
        MydataTradeDTO tradeDTO = MydataTradeDTO.builder()
                .ciHash(ciHash)
                .brokerName("증권사A")
                .tradeType(TradeType.BUY)
                .stockType(stockType)
                .fundCode(fundCode)
                .ticker(ticker)
                .qty(BigDecimal.TEN)
                .tradeDate(tradeDate)
                .amount(BigDecimal.valueOf(1_000_000))
                .build();
        mydataTradeMapper.insertTrade(tradeDTO);
    }
}