package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.request.MydataRiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountLimitUpdateRequestDTO;
import com.app.mydata.domain.mydata.dto.request.RiaAccountRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataRiaAccountResponseDTO;
import com.app.mydata.domain.mydata.service.MydataRiaAccountService;
import com.app.mydata.global.response.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mydata/ria-accounts")
public class MydataRiaAccountApi {

    private final MydataRiaAccountService mydataRiaAccountService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<List<MydataRiaAccountResponseDTO>>> getRiaAccounts(
            @Valid @RequestBody MydataRiaAccountRequestDTO request
    ) {
        List<MydataRiaAccountResponseDTO> result = mydataRiaAccountService.getAccountsByCiHash(request);
        return ResponseEntity.ok(ApiResponseDTO.of("myData RIA 계좌 조회 성공", result));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponseDTO<MydataRiaAccountResponseDTO>> createAccount(@Valid @RequestBody RiaAccountRequestDTO riaAccountRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.of("myData RIA 계좌 등록 성공", mydataRiaAccountService.createRiaAccount(riaAccountRequestDTO)));
    }

    @PutMapping("/limit-update")
    public ResponseEntity<ApiResponseDTO<MydataRiaAccountResponseDTO>> updateAccountLimit(@Valid @RequestBody RiaAccountLimitUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.of("myData RIA 계좌 한도 변경 성공", mydataRiaAccountService.updateRiaAccountLimit(request)));
    }
}
