package com.app.mydata.domain.mydata.mapper;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface MydataRiaAccountMapper {

    List<MydataRiaAccountDTO> selectByCiHash(@Param("ciHash") String ciHash);

    void insertAccount(MydataRiaAccountDTO accountDTO);

    void upsertAccount(MydataRiaAccountDTO accountDTO);

    Optional<MydataRiaAccountDTO> selectByCiHashAndBrokerName(MydataRiaAccountDTO riaAccountDTO);
}
