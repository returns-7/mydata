package com.app.mydata.domain.mydata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RiaAccountCreateResult {
    private final MydataRiaAccountResponseDTO account;
    private final boolean created;
}
