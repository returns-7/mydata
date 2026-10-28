package com.app.mydata.global.exception;

import com.app.mydata.domain.member.exception.MemberException;
import com.app.mydata.domain.member.exception.MemberNotFoundException;
import com.app.mydata.domain.mydata.exception.*;
import com.app.mydata.global.response.ApiResponseDTO;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 1. Member
  @ExceptionHandler(MemberException.class)
  public ResponseEntity<ApiResponseDTO<String>> handleException(MemberException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.of(e.getMessage()));
  }
  @ExceptionHandler(MemberNotFoundException.class)
  public ResponseEntity<ApiResponseDTO<Void>>handleMemberNotFound(MemberNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.of(e.getMessage()));
  }

  // 2. MydataTrade
  @ExceptionHandler(MydataTradeException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataTradeException(MydataTradeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.of(e.getMessage()));
  }
  @ExceptionHandler(MydataTradeNotFoundException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataTradeNotFound(MydataTradeNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.of(e.getMessage()));
  }

  // 3. MydataRiaAccount
  @ExceptionHandler(MydataRiaAccountException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataRiaAccountException(MydataRiaAccountException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.of(e.getMessage()));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    return validationError(e.getBindingResult());
  }
  @ExceptionHandler(MydataRiaAccountNotFoundException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataRiaAccountNotFound(MydataRiaAccountNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.of(e.getMessage()));
  }

  // 4. MydataFund
  @ExceptionHandler(MydataFundException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataFundException(MydataFundException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.of(e.getMessage()));
  }
  @ExceptionHandler(MydataFundNotFoundException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleMydataFundNotFound(MydataFundNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.of(e.getMessage()));
  }

  // 5. Validation
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleBindException(BindException e) {
    return validationError(e.getBindingResult());
  }

  private ResponseEntity<ApiResponseDTO<Void>> validationError(BindingResult bindingResult) {
    String message = bindingResult.getFieldErrors().stream()
            .findFirst()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .orElse("요청값이 올바르지 않습니다.");
    return ResponseEntity.badRequest().body(ApiResponseDTO.of(message));
  }
}
