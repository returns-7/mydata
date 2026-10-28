package com.app.mydata.domain.mydata.service;

import com.app.mydata.domain.mydata.dto.MydataRiaAccountDTO;
import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountException;
import com.app.mydata.domain.mydata.exception.MydataRiaAccountNotFoundException;
import com.app.mydata.domain.mydata.mapper.MydataKeyMapper;
import com.app.mydata.domain.mydata.mapper.MydataRiaAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MydataRiaAccountServiceImpl implements MydataRiaAccountService {

    private final MydataRiaAccountMapper mydataRiaAccountMapper;
    private final MydataKeyMapper mydataKeyMapper;
    private final MydataRiaAccountUpsertExecutor upsertExecutor;

    @Override
    public List<MydataRiaAccountResponseDTO> getAccountsByCiHash(
            MydataRiaAccountRequestDTO request) {
        String ciHash = request.getCiHash();
        if (mydataKeyMapper.existsByCiHash(ciHash) == 0) {
            throw new MydataRiaAccountNotFoundException("등록되지 않은 CiHash입니다.");
        }
        List<MydataRiaAccountDTO> accounts = mydataRiaAccountMapper.selectByCiHash(ciHash);
        return accounts.stream()
                .map(MydataRiaAccountResponseDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    public MydataRiaAccountResponseDTO syncRiaAccount(RiaAccountRequestDTO riaAccountRequestDTO) {
        MydataRiaAccountDTO riaAccountDTO = riaAccountRequestDTO.toDTO();
        if (mydataKeyMapper.existsByCiHash(riaAccountDTO.getCiHash()) == 0) {
            throw new MydataRiaAccountNotFoundException("등록되지 않은 사용자 입니다.");
        }

        upsertWithRetry(riaAccountDTO);

        MydataRiaAccountDTO savedAccount = mydataRiaAccountMapper.selectByCiHashAndBrokerName(riaAccountDTO)
                .orElseThrow(() -> new MydataRiaAccountException("재조회 실패"));
        return MydataRiaAccountResponseDTO.of(savedAccount);
    }

    private void upsertWithRetry(MydataRiaAccountDTO riaAccountDTO) {
        try {
            upsertExecutor.upsert(riaAccountDTO);
        } catch (DataIntegrityViolationException e) {
            // 동시 upsert 경합으로 유니크 제약 위반 시 1회 재시도.
            // REQUIRES_NEW라 방금 실패한(aborted) 트랜잭션과 분리된 새 트랜잭션으로 다시 시도한다.
            upsertExecutor.upsert(riaAccountDTO);
        }
    }
}