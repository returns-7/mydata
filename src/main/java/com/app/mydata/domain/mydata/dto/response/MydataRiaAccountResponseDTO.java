package com.app.mydata.domain.mydata.dto.response;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataRiaAccountResponseDTO {
    private Long mydataAccountId;
    private String ciHash;
    private String brokerName;
    private BigDecimal riaLimit;
    private BigDecimal riaCumulativeSell;

    public static MydataRiaAccountResponseDTO of(MydataRiaAccountDTO dto) {
        return MydataRiaAccountResponseDTO.builder()
                .mydataAccountId(dto.getMydataAccountId())
                .ciHash(dto.getCiHash())
                .brokerName(dto.getBrokerName())
                .riaLimit(dto.getRiaLimit())
                .riaCumulativeSell(dto.getRiaCumulativeSell())
                .build();
    }
}