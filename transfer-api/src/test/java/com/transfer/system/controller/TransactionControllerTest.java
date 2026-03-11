package com.transfer.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.CommonResponseAdvice;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.GlobalExceptionHandler;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.service.TransactionService;
import com.transfer.system.service.TransferFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransferFacade transferFacade;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private TransactionRequestDTO transactionRequestDTO;
    private TransactionEntity transactionEntity;
    private AccountEntity fromAccount;
    private AccountEntity toAccount;

    private final String testFromAccountNumber = "00125080800001";
    private final String testToAccountNumber = "00125080800002";

    private static class Endpoint {
        static final String TRANSFER = "/api/transaction/transfer";
        static final String HISTORY = "/api/transaction/history";
    }

    @BeforeEach
    void setUp() {
        TransactionController transactionController = new TransactionController(transactionService, transferFacade);
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
            .setControllerAdvice(new GlobalExceptionHandler(), new CommonResponseAdvice())
            .build();

        transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber(testFromAccountNumber)
            .toAccountNumber(testToAccountNumber)
            .amount(new BigDecimal("100000"))
            .build();

        fromAccount = AccountEntity.builder().accountNumber(testFromAccountNumber).build();
        toAccount = AccountEntity.builder().accountNumber(testToAccountNumber).build();

        transactionEntity = TransactionEntity.builder()
            .transactionId(UUID.randomUUID())
            .fromAccount(fromAccount)
            .toAccount(toAccount)
            .amount(new BigDecimal("100000"))
            .fee(new BigDecimal("1000"))
            .transactionType(TransactionType.TRANSFER)
            .build();
    }

    // ========================= 이체 API 테스트 =========================
    @Nested
    class TransferApiTest {

        @Test
        void transfer_success() throws Exception {
            when(transferFacade.transfer(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(transactionEntity);

            mockMvc.perform(post(Endpoint.TRANSFER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.data.fromAccountNumber").value(testFromAccountNumber));
        }

        @Test
        void transfer_missingFields() throws Exception {
            TransactionRequestDTO invalidDto = TransactionRequestDTO.builder()
                .fromAccountNumber("")
                .toAccountNumber(null)
                .amount(new BigDecimal("-100"))
                .build();

            mockMvc.perform(post(Endpoint.TRANSFER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result_code").value(ResultCode.FAIL_INVALID_PARAMETER.getCode()));
        }

        @Test
        void transfer_accountNotFound() throws Exception {
            when(transferFacade.transfer(anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(post(Endpoint.TRANSFER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequestDTO)))
                .andExpect(status().isNotFound());
        }
    }

    // ========================= 거래 내역 조회 API 테스트 =========================
    @Nested
    class HistoryApiTest {

        @Test
        void getHistory_success() throws Exception {
            Page<TransactionEntity> historyPage = new PageImpl<>(List.of(transactionEntity), PageRequest.of(0, 10), 1);
            when(transactionService.getTransactionHistory(anyString(), anyInt(), anyInt()))
                .thenReturn(historyPage);

            mockMvc.perform(get(Endpoint.HISTORY)
                .param("accountNumber", testFromAccountNumber)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].fromAccountNumber").value(testFromAccountNumber));
        }

        @Test
        void getHistory_missingAccountNumber() throws Exception {
            // 빈 계좌번호로 요청 시, MockMvcStandalone에선 서비스가 호출될 수 있음. 
            // 서비스에서 예외를 던지도록 설정하여 400 응답 유도.
            when(transactionService.getTransactionHistory(eq(""), anyInt(), anyInt()))
                .thenThrow(new TransferSystemException(ErrorCode.INVALID_ACCOUNT_NUMBER));

            mockMvc.perform(get(Endpoint.HISTORY)
                .param("accountNumber", "")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_ACCOUNT_NUMBER.getMessage()));
        }
    }
}
