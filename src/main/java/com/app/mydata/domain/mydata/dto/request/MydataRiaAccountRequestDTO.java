package com.app.mydata.domain.mydata.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @ToString @Builder
public class MydataRiaAccountRequestDTO {

    @NotBlank(message = "ciHash는 필수입니다.")
    private String ciHash;
}
