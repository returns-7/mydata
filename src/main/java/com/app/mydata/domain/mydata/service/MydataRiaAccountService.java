package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountLimitUpdateRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import java.util.List;

public interface MydataRiaAccountService {

    List<MydataRiaAccountResponseDTO> getAccountsByCiHash(
            MydataRiaAccountRequestDTO request
    );

    MydataRiaAccountResponseDTO createRiaAccount(RiaAccountRequestDTO riaAccountRequestDTO);

    MydataRiaAccountResponseDTO updateRiaAccountLimit(RiaAccountLimitUpdateRequestDTO request);
}
