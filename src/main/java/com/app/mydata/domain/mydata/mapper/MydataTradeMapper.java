package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataTradeDTO;
import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MydataTradeMapper {

    List<MydataTradeDTO> selectByCiHashAndPeriod(
            MydataTradeRequestDTO request);

    void insertTrade(MydataTradeDTO tradeDTO);
}
