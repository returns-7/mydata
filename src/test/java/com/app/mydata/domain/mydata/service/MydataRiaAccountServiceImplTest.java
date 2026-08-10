package com.app.mydata.domain.mydata.service;


import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountLimitUpdateRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import com.app.mydata.domain.mydata.dto.response.RiaAccountCreateResult;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountException;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountNotFoundException;
import com.app.mydata.domain.mydata.mapper.MydataKeyMapper;
import com.app.mydata.domain.mydata.mapper.MydataRiaAccountMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MydataRiaAccountServiceImplTest {

    @Mock
    private MydataRiaAccountMapper mydataRiaAccountMapper;

    @Mock
    private MydataKeyMapper mydataKeyMapper;

    @InjectMocks
    private MydataRiaAccountServiceImpl mydataRiaAccountService;

    @Test
    void getAccountsByCiHashReturnsAccountsWhenCiHashIsRegistered() {
        String ciHash = "test-ci-hash";
        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);

        MydataRiaAccountDTO dto = MydataRiaAccountDTO.builder()
                .mydataAccountId(1L)
                .ciHash(ciHash)
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataRiaAccountMapper.selectByCiHash(ciHash)).thenReturn(List.of(dto));

        List<MydataRiaAccountResponseDTO> result = mydataRiaAccountService.getAccountsByCiHash(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCiHash()).isEqualTo(ciHash);
        assertThat(result.get(0).getBrokerName()).isEqualTo("증권사A");
    }

    @Test
    void getAccountsByCiHashThrowsExceptionWhenCiHashIsUnregistered() {
        String ciHash = "unknown-ci-hash";
        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(0);

        assertThatThrownBy(() -> mydataRiaAccountService.getAccountsByCiHash(request))
                .isInstanceOf(MydataRiaAccountException.class)
                .hasMessage("등록되지 않은 CiHash입니다.");

        verifyNoInteractions(mydataRiaAccountMapper);
    }

    @Test
    void getAccountsByCiHashReturnsEmptyListWhenNoAccountsExist() {
        String ciHash = "test-ci-hash";
        MydataRiaAccountRequestDTO request = MydataRiaAccountRequestDTO.builder()
                .ciHash(ciHash)
                .build();

        when(mydataKeyMapper.existsByCiHash(ciHash)).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHash(ciHash)).thenReturn(List.of());

        List<MydataRiaAccountResponseDTO> result = mydataRiaAccountService.getAccountsByCiHash(request);

        assertThat(result).isEmpty();
    }

    @Test
    void createRiaAccountConvertsNullCumulativeSellToZeroAndInsertsNewAccount() {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(null)
                .build();
        MydataRiaAccountDTO savedAccount = MydataRiaAccountDTO.builder()
                .mydataAccountId(1L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedAccount));

        RiaAccountCreateResult result = mydataRiaAccountService.createRiaAccount(request);

        ArgumentCaptor<MydataRiaAccountDTO> captor = ArgumentCaptor.forClass(MydataRiaAccountDTO.class);
        verify(mydataRiaAccountMapper).insertAccount(captor.capture());
        assertThat(captor.getValue().getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.isCreated()).isTrue();
        assertThat(result.getAccount().getMydataAccountId()).isEqualTo(1L);
        assertThat(result.getAccount().getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createRiaAccountReturnsExistingAccountWithoutInsert() {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .build();
        MydataRiaAccountDTO existingAccount = MydataRiaAccountDTO.builder()
                .mydataAccountId(1L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(existingAccount));

        RiaAccountCreateResult result = mydataRiaAccountService.createRiaAccount(request);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getAccount().getMydataAccountId()).isEqualTo(1L);
        verify(mydataRiaAccountMapper, org.mockito.Mockito.never()).insertAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRiaAccountReturnsExistingAccountAfterConcurrentInsert() {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .build();
        MydataRiaAccountDTO existingAccount = MydataRiaAccountDTO.builder()
                .mydataAccountId(2L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAccount));
        org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate"))
                .when(mydataRiaAccountMapper).insertAccount(org.mockito.ArgumentMatchers.any());

        RiaAccountCreateResult result = mydataRiaAccountService.createRiaAccount(request);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getAccount().getMydataAccountId()).isEqualTo(2L);
    }

    @Test
    void updateRiaAccountLimitUpdatesOnlyLimitOfExistingAccount() {
        RiaAccountLimitUpdateRequestDTO request = RiaAccountLimitUpdateRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(40_000_000))
                .build();
        MydataRiaAccountDTO existingAccount = MydataRiaAccountDTO.builder()
                .mydataAccountId(7L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        MydataRiaAccountDTO updatedAccount = MydataRiaAccountDTO.builder()
                .mydataAccountId(7L)
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(40_000_000))
                .riaCumulativeSell(BigDecimal.ZERO)
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(existingAccount))
                .thenReturn(Optional.of(updatedAccount));

        MydataRiaAccountResponseDTO result = mydataRiaAccountService.updateRiaAccountLimit(request);

        ArgumentCaptor<MydataRiaAccountDTO> captor = ArgumentCaptor.forClass(MydataRiaAccountDTO.class);
        verify(mydataRiaAccountMapper).updateAccount(captor.capture());
        assertThat(captor.getValue().getMydataAccountId()).isEqualTo(7L);
        assertThat(captor.getValue().getRiaLimit()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        assertThat(captor.getValue().getRiaCumulativeSell()).isNull();
        assertThat(result.getMydataAccountId()).isEqualTo(7L);
        assertThat(result.getRiaLimit()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        assertThat(result.getRiaCumulativeSell()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createRiaAccountThrowsExceptionWhenCiHashIsUnregistered() {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("unknown-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .build();
        when(mydataKeyMapper.existsByCiHash("unknown-ci-hash")).thenReturn(0);

        assertThatThrownBy(() -> mydataRiaAccountService.createRiaAccount(request))
                .isInstanceOf(MydataRiaAccountException.class)
                .hasMessage("등록되지 않은 사용자 입니다.");

        verifyNoInteractions(mydataRiaAccountMapper);
    }

    @Test
    void createRiaAccountThrowsExceptionWhenReloadFails() {
        RiaAccountRequestDTO request = RiaAccountRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mydataRiaAccountService.createRiaAccount(request))
                .isInstanceOf(MydataRiaAccountException.class)
                .hasMessage("재조회 실패");

        verify(mydataRiaAccountMapper).insertAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRiaAccountLimitThrowsNotFoundWhenAccountDoesNotExist() {
        RiaAccountLimitUpdateRequestDTO request = RiaAccountLimitUpdateRequestDTO.builder()
                .ciHash("test-ci-hash")
                .brokerName("증권사A")
                .riaLimit(BigDecimal.valueOf(30_000_000))
                .build();
        when(mydataKeyMapper.existsByCiHash("test-ci-hash")).thenReturn(1);
        when(mydataRiaAccountMapper.selectByCiHashAndBrokerName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mydataRiaAccountService.updateRiaAccountLimit(request))
                .isInstanceOf(MydataRiaAccountNotFoundException.class)
                .hasMessage("등록되지 않은 RIA 계좌입니다.");
    }
}
