package com.app.mydata.domain.mydata.api;

import com.app.mydata.domain.mydata.dto.request.MydataTradeRequestDTO;
import com.app.mydata.domain.mydata.dto.response.MydataTradeResponseDTO;
import com.app.mydata.domain.mydata.service.MydataTradeService;
import com.app.mydata.global.response.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mydata/trades")
public class MydataTradeApi {

    private final MydataTradeService mydataTradeService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<List<MydataTradeResponseDTO>>> getTrades(
            @Valid @RequestBody MydataTradeRequestDTO request
    ) {
        List<MydataTradeResponseDTO> result = mydataTradeService.getTradesByCiHash(request);
        return ResponseEntity.ok(ApiResponseDTO.of("myData 거래 내역 조회 성공", result));
    }
}