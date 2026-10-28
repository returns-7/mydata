package com.app.mydata.domain.mydata.dto;

import lombok.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataRiaAccountDTO {
    private Long mydataAccountId;
    private String ciHash;
    private String brokerName;
    private BigDecimal riaLimit;
    private BigDecimal riaCumulativeSell;
}
