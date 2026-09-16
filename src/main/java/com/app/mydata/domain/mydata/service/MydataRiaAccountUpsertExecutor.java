package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import com.app.mydata.domain.mydata.mapper.MydataRiaAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MydataRiaAccountUpsertExecutor {

    private final MydataRiaAccountMapper mydataRiaAccountMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(MydataRiaAccountDTO riaAccountDTO) {
        mydataRiaAccountMapper.upsertAccount(riaAccountDTO);
    }
}