package com.app.mydata.domain.mydata.dto.request;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiaAccountLimitUpdateRequestDTO {
    @NotBlank
    private String ciHash;

    @NotBlank
    private String brokerName;

    @NotNull
    @Positive
    private BigDecimal riaLimit;

    public MydataRiaAccountDTO toDTO() {
        return MydataRiaAccountDTO.builder()
                .ciHash(ciHash)
                .brokerName(brokerName)
                .riaLimit(riaLimit)
                .build();
    }
}
