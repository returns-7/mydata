package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataTradeResponseDTO;
import java.util.List;

public interface MydataTradeService {

    List<MydataTradeResponseDTO> getTradesByCiHash(
            MydataTradeRequestDTO request);
}
