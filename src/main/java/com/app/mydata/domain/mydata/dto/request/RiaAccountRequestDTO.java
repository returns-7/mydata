package com.app.mydata.domain.mydata.dto.request;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiaAccountRequestDTO {
  @NotBlank
  private String ciHash;
  @NotBlank
  private String brokerName;
  @NotNull
  @DecimalMin(value = "1", inclusive = true)
  @DecimalMax(value = "50000000", inclusive = true)
  @Digits(integer = 8, fraction = 0)
  private BigDecimal riaLimit;
  public MydataRiaAccountDTO toDTO(){
    return MydataRiaAccountDTO.builder()
        .ciHash(this.getCiHash())
        .brokerName(this.getBrokerName())
        .riaLimit(this.getRiaLimit())
        .build();
  }
}
