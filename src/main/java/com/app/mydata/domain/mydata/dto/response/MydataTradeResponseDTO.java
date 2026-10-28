package com.app.mydata.domain.mydata.dto.response;

import com.app.mydata.domain.mydata.dto.MydataTradeDTO;
import com.app.mydata.domain.mydata.type.StockType;
import com.app.mydata.domain.mydata.type.TradeType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataTradeResponseDTO {

    private Long tradeId;
    private String ciHash;
    private String brokerName;
    private TradeType tradeType;
    private StockType stockType;
    private BigDecimal qty;
    private LocalDate tradeDate;
    private BigDecimal amount;
    private String fundCode;
    private String ticker;

    public static MydataTradeResponseDTO of(MydataTradeDTO dto) {
        return MydataTradeResponseDTO.builder()
                .tradeId(dto.getTradeId())
                .ciHash(dto.getCiHash())
                .brokerName(dto.getBrokerName())
                .tradeType(dto.getTradeType())
                .stockType(dto.getStockType())
                .qty(dto.getQty())
                .tradeDate(dto.getTradeDate())
                .amount(dto.getAmount())
                .fundCode(dto.getFundCode())
                .ticker(dto.getTicker())
                .build();
    }
}