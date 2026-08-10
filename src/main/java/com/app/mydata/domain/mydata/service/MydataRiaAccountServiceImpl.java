package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountLimitUpdateRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import com.app.mydata.domain.mydata.dto.response.RiaAccountCreateResult;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountException;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountNotFoundException;
import com.app.mydata.domain.mydata.mapper.MydataKeyMapper;
import com.app.mydata.domain.mydata.mapper.MydataRiaAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MydataRiaAccountServiceImpl implements MydataRiaAccountService {

    private final MydataRiaAccountMapper mydataRiaAccountMapper;
    private final MydataKeyMapper mydataKeyMapper;

    @Override
    public List<MydataRiaAccountResponseDTO> getAccountsByCiHash(
            MydataRiaAccountRequestDTO request) {

        String ciHash = request.getCiHash();

        if (mydataKeyMapper.existsByCiHash(ciHash) == 0) {
            throw new MydataRiaAccountException("등록되지 않은 CiHash입니다.");
        }

        List<MydataRiaAccountDTO> accounts = mydataRiaAccountMapper.selectByCiHash(ciHash);

        return accounts.stream()
                .map(MydataRiaAccountResponseDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RiaAccountCreateResult createRiaAccount(RiaAccountRequestDTO riaAccountRequestDTO) {
        MydataRiaAccountDTO riaAccountDTO = riaAccountRequestDTO.toDTO();
        if (mydataKeyMapper.existsByCiHash(riaAccountDTO.getCiHash()) == 0) {
            throw new MydataRiaAccountException("등록되지 않은 사용자 입니다.");
        }

        MydataRiaAccountDTO existingAccount = mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO).orElse(null);
        if (existingAccount != null) {
            return new RiaAccountCreateResult(MydataRiaAccountResponseDTO.of(existingAccount), false);
        }

        try {
            mydataRiaAccountMapper.insertAccount(riaAccountDTO);
        } catch (DuplicateKeyException e) {
            MydataRiaAccountDTO concurrentAccount = mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO).orElseThrow(() -> new MydataRiaAccountException("중복 생성 계좌 재조회 실패"));
            return new RiaAccountCreateResult(MydataRiaAccountResponseDTO.of(concurrentAccount), false);
        }

        MydataRiaAccountDTO createdAccount = mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO).orElseThrow(() -> new MydataRiaAccountException("재조회 실패"));
        return new RiaAccountCreateResult(MydataRiaAccountResponseDTO.of(createdAccount), true);
    }

    @Override
    public MydataRiaAccountResponseDTO updateRiaAccountLimit(RiaAccountLimitUpdateRequestDTO request) {
        MydataRiaAccountDTO riaAccountDTO = request.toDTO();
        if (mydataKeyMapper.existsByCiHash(riaAccountDTO.getCiHash()) == 0) {
            throw new MydataRiaAccountException("등록되지 않은 사용자 입니다.");
        }

        MydataRiaAccountDTO foundAccount = mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO)
                .orElseThrow(() -> new MydataRiaAccountNotFoundException("등록되지 않은 RIA 계좌입니다."));
        riaAccountDTO.setMydataAccountId(foundAccount.getMydataAccountId());
        mydataRiaAccountMapper.updateAccount(riaAccountDTO);

        return MydataRiaAccountResponseDTO.of(mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO)
                .orElseThrow(() -> new MydataRiaAccountException("재조회 실패")));
    }
}
