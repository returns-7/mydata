package com.app.mydata.domain.mydata.dto;

import com.app.mydata.domain.mydata.type.StockType;
import com.app.mydata.domain.mydata.type.TradeType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataTradeDTO {

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
}
