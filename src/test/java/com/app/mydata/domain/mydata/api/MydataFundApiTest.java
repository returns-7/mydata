package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.response.MydataFundResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataFundNotFoundException;
import com.app.mydata.domain.mydata.service.MydataFundService;
import com.app.mydata.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MydataFundApiTest {

    private MydataFundService mydataFundService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mydataFundService = mock(MydataFundService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MydataFundApi(mydataFundService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getFundReturnsFundWhenFundCodeExists() throws Exception {
        MydataFundResponseDTO response = MydataFundResponseDTO.builder()
                .fundCode("448630")
                .fundName("TIGER 미국배당다우존스")
                .foreignStockRatio(BigDecimal.valueOf(72.50))
                .inceptionDate(LocalDate.of(2023, 5, 10))
                .build();
        when(mydataFundService.getFundByCode("448630")).thenReturn(response);

        mockMvc.perform(get("/api/mydata/funds/{fundCode}", "448630"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("펀드 정보 조회 성공"))
                .andExpect(jsonPath("$.data.fundCode").value("448630"))
                .andExpect(jsonPath("$.data.fundName").value("TIGER 미국배당다우존스"))
                .andExpect(jsonPath("$.data.foreignStockRatio").value(72.50));

        verify(mydataFundService).getFundByCode(eq("448630"));
    }

    @Test
    void getFundReturnsNotFoundWhenFundCodeDoesNotExist() throws Exception {
        when(mydataFundService.getFundByCode("999999"))
                .thenThrow(new MydataFundNotFoundException("존재하지 않는 fundCode입니다: 999999"));

        mockMvc.perform(get("/api/mydata/funds/{fundCode}", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 fundCode입니다: 999999"));
    }
}