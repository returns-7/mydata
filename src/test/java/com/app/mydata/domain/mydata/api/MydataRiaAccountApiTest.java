package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountLimitUpdateRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountException;
import com.app.mydata.domain.mydata.service.MydataRiaAccountService;
import com.app.mydata.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MydataRiaAccountApiTest {

    private MydataRiaAccountService mydataRiaAccountService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mydataRiaAccountService = mock(MydataRiaAccountService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MydataRiaAccountApi(mydataRiaAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRiaAccountsAcceptsValidRequest() throws Exception {
        MydataRiaAccountResponseDTO response = MydataRiaAccountResponseDTO.builder()
                .mydataAccountId(1L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .build();
        when(mydataRiaAccountService.getAccountsByCiHash(any())).thenReturn(List.of(response));

        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("myData RIA 계좌 조회 성공"))
                .andExpect(jsonPath("$.data[0].mydataAccountId").value(1));

        verify(mydataRiaAccountService).getAccountsByCiHash(any());
    }

    @Test
    void getRiaAccountsRejectsMissingCiHash() throws Exception {
        mockMvc.perform(post("/api/mydata/ria-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ciHash는 필수입니다."));

        verify(mydataRiaAccountService, never()).getAccountsByCiHash(any());
    }

    @Test
    void getRiaAccountsRejectsBlankCiHash() throws Exception {
        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash("")
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ciHash는 필수입니다."));

        verify(mydataRiaAccountService, never()).getAccountsByCiHash(any());
    }

    @Test
    void getRiaAccountsReturnsBadRequestWhenCiHashUnregistered() throws Exception {
        when(mydataRiaAccountService.getAccountsByCiHash(any()))
                .thenThrow(new MydataRiaAccountException("등록되지 않은 CiHash입니다."));

        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash("unknown-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("등록되지 않은 CiHash입니다."));
    }

    @Test
    void getRiaAccountsReturnsEmptyListWhenNoAccountsExist() throws Exception {
        when(mydataRiaAccountService.getAccountsByCiHash(any())).thenReturn(List.of());

        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createRiaAccountAcceptsNullCumulativeSell() throws Exception {
        MydataRiaAccountResponseDTO response = MydataRiaAccountResponseDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataRiaAccountService.createRiaAccount(any())).thenReturn(response);

        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(null)
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("myData RIA 계좌 등록 성공"))
                .andExpect(jsonPath("$.data.riaCumulativeSell").value(0));

        verify(mydataRiaAccountService).createRiaAccount(any());
    }

    @Test
    void createRiaAccountRejectsNegativeCumulativeSell() throws Exception {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.valueOf(-1))
                .build();

        mockMvc.perform(post("/api/mydata/ria-accounts/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(mydataRiaAccountService, never()).createRiaAccount(any());
    }

    @Test
    void updateRiaAccountLimitUsesPutLimitUpdateEndpoint() throws Exception {
        MydataRiaAccountResponseDTO response = MydataRiaAccountResponseDTO.builder()
                .mydataAccountId(1L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(40_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataRiaAccountService.updateRiaAccountLimit(any())).thenReturn(response);

        RiaAccountLimitUpdateRequestDTO request = RiaAccountLimitUpdateRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(40_000_000))
                .build();

        mockMvc.perform(put("/api/mydata/ria-accounts/limit-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("myData RIA 계좌 한도 변경 성공"))
                .andExpect(jsonPath("$.data.riaLimit").value(40_000_000));

        verify(mydataRiaAccountService).updateRiaAccountLimit(any());
    }
}
