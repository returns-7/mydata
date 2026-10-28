package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataTradeResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataTradeException;
import com.app.mydata.domain.mydata.service.MydataTradeService;
import com.app.mydata.domain.mydata.type.StockType;
import com.app.mydata.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MydataTradeApiTest {

    private MydataTradeService mydataTradeService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mydataTradeService = mock(MydataTradeService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MydataTradeApi(mydataTradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTradesAcceptsValidRequestWithDateRange() throws Exception {
        MydataTradeResponseDTO response = MydataTradeResponseDTO.builder()
                .tradeId(1L)
                .ciHash("test-ci-hash")
                .build();
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of(response));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("test-ci-hash")
                .fromDate(LocalDate.of(2026, 1, 1))
                .toDate(LocalDate.of(2026, 12, 31))
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("myData 거래 내역 조회 성공"))
                .andExpect(jsonPath("$.data[0].tradeId").value(1));

        verify(mydataTradeService).getTradesByCiHash(any());
    }

    @Test
    void getTradesAcceptsRequestWithoutDateRange() throws Exception {
        MydataTradeResponseDTO response = MydataTradeResponseDTO.builder()
                .tradeId(1L)
                .ciHash("test-ci-hash")
                .build();
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of(response));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mydataTradeService).getTradesByCiHash(any());
    }

    @Test
    void getTradesIncludesFundCodeForFundTrades() throws Exception {
        MydataTradeResponseDTO response = MydataTradeResponseDTO.builder()
                .tradeId(2L)
                .ciHash("test-ci-hash")
                .stockType(StockType.FUND)
                .fundCode("448630")
                .build();
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of(response));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockType").value("FUND"))
                .andExpect(jsonPath("$.data[0].fundCode").value("448630"));
    }

    @Test
    void getTradesIncludesTickerForNonFundTrades() throws Exception {
        MydataTradeResponseDTO response = MydataTradeResponseDTO.builder()
                .tradeId(3L)
                .ciHash("test-ci-hash")
                .stockType(StockType.FOREIGN_STOCK)
                .ticker("AAPL")
                .build();
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of(response));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockType").value("FOREIGN_STOCK"))
                .andExpect(jsonPath("$.data[0].ticker").value("AAPL"));
    }

    @Test
    void getTradesParsesFromDateWhenSentAsJsonArray() throws Exception {
        // maria의 RestClient는 Spring Boot가 커스터마이징하지 않은 기본 ObjectMapper를 쓰기 때문에
        // LocalDate를 ISO 문자열이 아니라 [year,month,day] 배열로 직렬화해서 보낸다.
        // 이 요청이 실제로 mydata 컨트롤러에서 올바르게 역직렬화되는지 검증한다.
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of());

        String rawBody = "{\"ciHash\":\"test-ci-hash\",\"fromDate\":[2026,3,1]}";

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBody))
                .andExpect(status().isOk());

        ArgumentCaptor<MydataTradeRequestDTO> captor = ArgumentCaptor.forClass(MydataTradeRequestDTO.class);
        verify(mydataTradeService).getTradesByCiHash(captor.capture());
        assertThat(captor.getValue().getFromDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void getTradesRejectsMissingCiHash() throws Exception {
        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ciHash는 필수입니다."));

        verify(mydataTradeService, never()).getTradesByCiHash(any());
    }

    @Test
    void getTradesRejectsBlankCiHash() throws Exception {
        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ciHash는 필수입니다."));

        verify(mydataTradeService, never()).getTradesByCiHash(any());
    }

    @Test
    void getTradesReturnsBadRequestWhenCiHashUnregistered() throws Exception {
        when(mydataTradeService.getTradesByCiHash(any()))
                .thenThrow(new MydataTradeException("등록되지 않은 CiHash입니다."));

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("unknown-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("등록되지 않은 CiHash입니다."));
    }

    @Test
    void getTradesReturnsEmptyListWhenNoTradesExist() throws Exception {
        when(mydataTradeService.getTradesByCiHash(any())).thenReturn(List.of());

        MydataTradeRequestDTO request = MydataTradeRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}