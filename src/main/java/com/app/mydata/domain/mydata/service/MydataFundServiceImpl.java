package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataFundDTO;
import com.app.mydata.domain.mydata.dto.response.MydataFundResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataFundNotFoundException;
import com.app.mydata.domain.mydata.mapper.MydataFundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MydataFundServiceImpl implements MydataFundService {

    private final MydataFundMapper mydataFundMapper;

    @Override
    public MydataFundResponseDTO getFundByCode(String fundCode) {
        MydataFundDTO fund = mydataFundMapper.selectByFundCode(fundCode)
                .orElseThrow(() -> new MydataFundNotFoundException("존재하지 않는 fundCode입니다: " + fundCode));

        return MydataFundResponseDTO.of(fund);
    }
}
