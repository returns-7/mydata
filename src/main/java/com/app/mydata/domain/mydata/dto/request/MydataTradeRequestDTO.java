package com.app.mydata.domain.mydata.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataTradeRequestDTO {

    @NotBlank(message = "ciHash는 필수입니다.")
    private String ciHash;
    
    private LocalDate fromDate;
    private LocalDate toDate;
}
