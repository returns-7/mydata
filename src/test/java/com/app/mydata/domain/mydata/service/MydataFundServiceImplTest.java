package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataFundDTO;
import com.app.mydata.domain.mydata.dto.response.MydataFundResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataFundNotFoundException;
import com.app.mydata.domain.mydata.mapper.MydataFundMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MydataFundServiceImplTest {

    @Mock
    private MydataFundMapper mydataFundMapper;

    @InjectMocks
    private MydataFundServiceImpl mydataFundService;

    @Test
    void getFundByCodeReturnsFundWhenFundCodeExists() {
        String fundCode = "448630";
        MydataFundDTO dto = MydataFundDTO.builder()
                .fundCode(fundCode)
                .fundName("TIGER 미국배당다우존스")
                .foreignStockRatio(BigDecimal.valueOf(72.50))
                .inceptionDate(LocalDate.of(2023, 5, 10))
                .build();
        when(mydataFundMapper.selectByFundCode(fundCode)).thenReturn(Optional.of(dto));

        MydataFundResponseDTO result = mydataFundService.getFundByCode(fundCode);

        assertThat(result.getFundCode()).isEqualTo(fundCode);
        assertThat(result.getFundName()).isEqualTo("TIGER 미국배당다우존스");
        assertThat(result.getForeignStockRatio()).isEqualByComparingTo(BigDecimal.valueOf(72.50));
        assertThat(result.getInceptionDate()).isEqualTo(LocalDate.of(2023, 5, 10));
    }

    @Test
    void getFundByCodeThrowsExceptionWhenFundCodeNotFound() {
        String fundCode = "999999";
        when(mydataFundMapper.selectByFundCode(fundCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mydataFundService.getFundByCode(fundCode))
                .isInstanceOf(MydataFundNotFoundException.class)
                .hasMessage("존재하지 않는 fundCode입니다: " + fundCode);
    }
}