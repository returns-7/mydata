package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.response.MydataFundResponseDTO;

public interface MydataFundService {

    MydataFundResponseDTO getFundByCode(String fundCode);
}
