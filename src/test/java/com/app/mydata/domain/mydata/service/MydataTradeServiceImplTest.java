package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataTradeDTO;
import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataTradeResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataTradeException;
import com.app.mydata.domain.mydata.mapper.MydataKeyMapper;
import com.app.mydata.domain.mydata.mapper.MydataTradeMapper;
import com.app.mydata.domain.mydata.type.StockType;
import com.app.mydata.domain.mydata.type.TradeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MydataTradeServiceImplTest {

    @Mock
    private MydataTradeMapper mydataTradeMapper;

    @Mock
    private MydataKeyMapper mydataKeyMapper;

    @InjectMocks
    private MydataTradeServiceImpl mydataTradeService;

    @Test
    void getTradesByCiHashReturnsTradesWhenCiHashIsRegistered() {
        String ciHash = "test-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .fromDate(LocalDate.of(2026, 1, 1))
                .toDate(LocalDate.of(2026, 12, 31))
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);

        MydataTradeDTO dto = MydataTradeDTO.builder()
                .tradeId(1L)
                .ciHash(ciHash)
                .brokerName("증권사A")
                .tradeType(TradeType.BUY)
                .stockType(StockType.FOREIGN_STOCK)
                .qty(BigDecimal.TEN)
                .tradeDate(LocalDate.of(2026, 3, 5))
                .amount(BigDecimal.valueOf(1_000_000))
                .build();
        when(mydataTradeMapper.selectByCiHashAndPeriod(request)).thenReturn(List.of(dto));

        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCiHash()).isEqualTo(ciHash);
        assertThat(result.get(0).getTradeType()).isEqualTo(TradeType.BUY);
    }

    @Test
    void getTradesByCiHashThrowsExceptionWhenCiHashIsUnregistered() {
        String ciHash = "unknown-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(0);

        assertThatThrownBy(() -> mydataTradeService.getTradesByCiHash(request))
                .isInstanceOf(MydataTradeException.class)
                .hasMessage("등록되지 않은 CiHash입니다.");

        verifyNoInteractions(mydataTradeMapper);
    }

    @Test
    void getTradesByCiHashPropagatesFundCodeForFundTrades() {
        String ciHash = "test-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);

        MydataTradeDTO dto = MydataTradeDTO.builder()
                .tradeId(2L)
                .ciHash(ciHash)
                .brokerName("증권사A")
                .tradeType(TradeType.BUY)
                .stockType(StockType.FUND)
                .fundCode("448630")
                .qty(BigDecimal.ONE)
                .tradeDate(LocalDate.of(2026, 3, 10))
                .amount(BigDecimal.valueOf(500_000))
                .build();
        when(mydataTradeMapper.selectByCiHashAndPeriod(request)).thenReturn(List.of(dto));

        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockType()).isEqualTo(StockType.FUND);
        assertThat(result.get(0).getFundCode()).isEqualTo("448630");
    }

    @Test
    void getTradesByCiHashPropagatesTickerForNonFundTrades() {
        String ciHash = "test-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);

        MydataTradeDTO dto = MydataTradeDTO.builder()
                .tradeId(4L)
                .ciHash(ciHash)
                .brokerName("증권사A")
                .tradeType(TradeType.BUY)
                .stockType(StockType.FOREIGN_STOCK)
                .ticker("AAPL")
                .qty(BigDecimal.TEN)
                .tradeDate(LocalDate.of(2026, 3, 5))
                .amount(BigDecimal.valueOf(1_000_000))
                .build();
        when(mydataTradeMapper.selectByCiHashAndPeriod(request)).thenReturn(List.of(dto));

        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("AAPL");
    }

    @Test
    void getTradesByCiHashReturnsNullFundCodeForNonFundTrades() {
        String ciHash = "test-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);

        MydataTradeDTO dto = MydataTradeDTO.builder()
                .tradeId(3L)
                .ciHash(ciHash)
                .brokerName("증권사A")
                .tradeType(TradeType.BUY)
                .stockType(StockType.FOREIGN_STOCK)
                .fundCode(null)
                .qty(BigDecimal.TEN)
                .tradeDate(LocalDate.of(2026, 3, 5))
                .amount(BigDecimal.valueOf(1_000_000))
                .build();
        when(mydataTradeMapper.selectByCiHashAndPeriod(request)).thenReturn(List.of(dto));

        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFundCode()).isNull();
    }

    @Test
    void getTradesByCiHashReturnsEmptyListWhenNoTradesExist() {
        String ciHash = "test-ci-hash";
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);
        when(mydataTradeMapper.selectByCiHashAndPeriod(request)).thenReturn(List.of());

        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);

        assertThat(result).isEmpty();
    }
}