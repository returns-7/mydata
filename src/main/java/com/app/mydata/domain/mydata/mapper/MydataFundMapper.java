package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataFundDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface MydataFundMapper {

    Optional<MydataFundDTO> selectByFundCode(String fundCode);
}
