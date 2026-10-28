package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.response.MydataFundResponseDTO;
import com.app.mydata.domain.mydata.service.MydataFundService;
import com.app.mydata.global.response.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mydata/funds")
public class MydataFundApi {

    private final MydataFundService mydataFundService;

    @GetMapping("/{fundCode}")
    public ResponseEntity<ApiResponseDTO<MydataFundResponseDTO>> getFund(
            @PathVariable String fundCode
    ) {
        MydataFundResponseDTO result = mydataFundService.getFundByCode(fundCode);
        return ResponseEntity.ok(ApiResponseDTO.of("펀드 정보 조회 성공", result));
    }
}
