package com.app.mydata.domain.mydata.service;


import com.app.mydata.domain.mydata.dto.MydataTradeDTO;
import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataTradeResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataTradeException;
import com.app.mydata.domain.mydata.mapper.MydataKeyMapper;
import com.app.mydata.domain.mydata.mapper.MydataTradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MydataTradeServiceImpl implements MydataTradeService {

    private final MydataTradeMapper mydataTradeMapper;
    private final MydataKeyMapper mydataKeyMapper;

    @Override
    public List<MydataTradeResponseDTO> getTradesByCiHash(
            MydataTradeRequestDTO request) {

        String ciHash = request.getCiHash();

        if (mydataKeyMapper.existsByCiHash(ciHash) == 0) {
            throw new MydataTradeException("등록되지 않은 CiHash입니다.");
        }

        List<MydataTradeDTO> trades = mydataTradeMapper.selectByCiHashAndPeriod(request);

        return trades.stream()
                .map(MydataTradeResponseDTO::of)
                .collect(Collectors.toList());
    }
}