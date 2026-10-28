package com.app.mydata.domain.mydata.dto.response;

import com.app.mydata.domain.mydata.dto.MydataFundDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class MydataFundResponseDTO {

    private String fundCode;
    private String fundName;
    private BigDecimal foreignStockRatio;
    private LocalDate inceptionDate;

    public static MydataFundResponseDTO of (MydataFundDTO dto) {
        return MydataFundResponseDTO.builder()
                .fundCode(dto.getFundCode())
                .fundName(dto.getFundName())
                .foreignStockRatio(dto.getForeignStockRatio())
                .inceptionDate(dto.getInceptionDate())
                .build();
    }
}
