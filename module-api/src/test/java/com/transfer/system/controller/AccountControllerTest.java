package com.transfer.system.controller;

import com.transfer.system.dto.AccountBalanceRequestDTO;
import com.transfer.system.dto.AccountBalanceResponseDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.exception.CommonResponseAdvice;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.GlobalExceptionHandler;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfer.system.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AccountCreateRequestDTO accountCreateRequestDTO;
    private AccountResponseDTO accountResponseDTO;

    private final UUID testAccountId = UUID.randomUUID();
    private final String testAccountNumber = "00125080800001";

    private static class Endpoint {
        static final String CREATE = "/api/account/create";
        static final String GET = "/api/account/{accountId}";
        static final String DELETE = "/api/account/{accountId}";
        static final String DEPOSIT = "/api/account/deposit";
        static final String WITHDRAW = "/api/account/withdraw";
    }

    @BeforeEach
    void setUp() {
        AccountController accountController = new AccountController(accountService);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
            .setControllerAdvice(new GlobalExceptionHandler(), new CommonResponseAdvice())
            .build();

        accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountName("mxxikr")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .build();

        accountResponseDTO = AccountResponseDTO.builder()
            .accountId(testAccountId)  
            .accountNumber(testAccountNumber)
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("100000"))
            .accountStatus(AccountStatus.ACTIVE)
            .createdTimeStamp(TimeUtils.nowKstLocalDateTime())
            .build();
    }

    // ========================== 공통 메서드 =========================

    /**
     * 계좌 생성 요청 수행
     */
    private ResultActions performCreateRequest(AccountCreateRequestDTO dto) throws Exception {
        return mockMvc.perform(post(Endpoint.CREATE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)));
    }

    /**
     * 계좌 조회 요청 수행
     */
    private ResultActions performGetRequest(UUID accountId) throws Exception {
        return mockMvc.perform(get(Endpoint.GET, accountId)
            .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 계좌 삭제 요청 수행
     */
    private ResultActions performDeleteRequest(UUID accountId) throws Exception {
        return mockMvc.perform(delete(Endpoint.DELETE, accountId)
            .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 계좌 입출금 요청 수행
     */
    private ResultActions performBalanceRequest(String endpoint, AccountBalanceRequestDTO dto) throws Exception {
        return mockMvc.perform(post(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)));
    }

    /**
     * 계좌 생성 시 예외 처리
     */
    private void expectCreateError(AccountCreateRequestDTO dto, ErrorCode errorCode, HttpStatus status) throws Exception {
        when(accountService.createAccount(any(AccountCreateRequestDTO.class)))
            .thenThrow(new TransferSystemException(errorCode));

        performCreateRequest(dto)
            .andExpect(status().is(status.value()))
            .andExpect(jsonPath("$.message").value(errorCode.getMessage()));

        verify(accountService).createAccount(any(AccountCreateRequestDTO.class));
    }

    private void expectCreateError_serviceNotCall(AccountCreateRequestDTO dto, HttpStatus status) throws Exception {
        performCreateRequest(dto)
            .andExpect(status().is(status.value()))
            .andExpect(jsonPath("$.result_code").value(ResultCode.FAIL_INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(accountService, never()).createAccount(any());
    }

    /**
     * 계좌 입출금 시 잔액 관련 예외 처리
     */
    private void expectBalanceError(String endpoint, AccountBalanceRequestDTO dto, ErrorCode errorCode, HttpStatus status) throws Exception {
        if (endpoint.equals(Endpoint.DEPOSIT)) {
            when(accountService.deposit(anyString(), any(BigDecimal.class)))
                .thenThrow(new TransferSystemException(errorCode));
        } else {
            when(accountService.withdraw(anyString(), any(BigDecimal.class)))
                .thenThrow(new TransferSystemException(errorCode));
        }

        performBalanceRequest(endpoint, dto)
            .andExpect(status().is(status.value()))
            .andExpect(jsonPath("$.message").value(errorCode.getMessage()));

        if (endpoint.equals(Endpoint.DEPOSIT)) {
            verify(accountService).deposit(eq(dto.getAccountNumber()), eq(dto.getAmount()));
        } else {
            verify(accountService).withdraw(eq(dto.getAccountNumber()), eq(dto.getAmount()));
        }
    }

    // ========================= 계좌 생성 테스트 =========================
    @Nested
    class CreateAccountTest {
        /**
         * 계좌 생성 성공
         */
        @Test
        void createAccount_success() throws Exception {
            when(accountService.createAccount(any(AccountCreateRequestDTO.class)))
                    .thenReturn(accountResponseDTO);

            performCreateRequest(accountCreateRequestDTO)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseMessage.ACCOUNT_CREATED.getMessage()))
                .andExpect(jsonPath("$.data.accountNumber").value(testAccountNumber))
                .andExpect(jsonPath("$.data.accountName").value("mxxikr"));

            verify(accountService).createAccount(any(AccountCreateRequestDTO.class));
        }

        /**
         * 계좌 생성 시 유효하지 않은 JSON 형식
         */
        @Test
        void createAccount_invalidJson() throws Exception {
            mockMvc.perform(post(Endpoint.CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result_code").value(ResultCode.ERROR_SERVER.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));

            verify(accountService, never()).createAccount(any());
        }

        /**
         * 계좌 생성 시 중복된 계좌 번호
         */
        @Test
        void createAccount_duplicateAccount() throws Exception {
            expectCreateError(accountCreateRequestDTO, ErrorCode.DUPLICATE_ACCOUNT_NUMBER, HttpStatus.CONFLICT);
        }

        /**
         * 계좌 생성 시 필드 누락
         */
        @Test
        void createAccount_invalidRequest() throws Exception {
            AccountCreateRequestDTO invalidRequest = AccountCreateRequestDTO.builder()
                .accountName("mxxikr")
                .build();

            expectCreateError_serviceNotCall(invalidRequest, HttpStatus.BAD_REQUEST);
        }
    }

    // ========================= 계좌 조회 테스트 =========================
    @Nested
    class GetAccountTest {

        /**
         * 계좌 조회 성공
         */
        @Test
        void getAccount_success() throws Exception {
            when(accountService.getAccount(testAccountId)).thenReturn(accountResponseDTO);

            performGetRequest(testAccountId)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseMessage.ACCOUNT_RETRIEVED.getMessage()))
                .andExpect(jsonPath("$.data.accountNumber").value(testAccountNumber));

            verify(accountService).getAccount(testAccountId);
        }

        /**
         * 계좌 조회 시 계좌가 존재하지 않는 경우
         */
        @Test
        void getAccount_notFound() throws Exception {
            when(accountService.getAccount(testAccountId))
                .thenThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

            performGetRequest(testAccountId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()));

            verify(accountService).getAccount(testAccountId);
        }

        /**
         * 계좌 조회 시 UUID 형식이 잘못된 경우
         */
        @Test
        void getAccount_invalidUUID() throws Exception {
            mockMvc.perform(get(Endpoint.GET, "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result_code").value(ResultCode.ERROR_SERVER.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));

            verify(accountService, never()).getAccount(any());
        }
    }

    // ========================= 계좌 삭제 테스트 =========================
    @Nested
    class DeleteAccountTest {

        /**
         * 계좌 삭제 성공
         */
        @Test
        void deleteAccount_success() throws Exception {
            doNothing().when(accountService).deleteAccount(testAccountId);  

            performDeleteRequest(testAccountId)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_NO_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseMessage.ACCOUNT_DELETED.getMessage()));

            verify(accountService).deleteAccount(testAccountId);
        }

        /**
         * 계좌 삭제 시 계좌가 존재하지 않는 경우
         */
        @Test
        void deleteAccount_notFound() throws Exception {
            doThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND))
                .when(accountService).deleteAccount(testAccountId);  

            performDeleteRequest(testAccountId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()));

            verify(accountService).deleteAccount(testAccountId);
        }
    }

    // ========================= 입출금 테스트 =========================
    @Nested
    class BalanceTest {

        /**
         * 계좌 입금 성공
         */
        @Test
        void deposit_success() throws Exception {
            BigDecimal amount = new BigDecimal("50000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(testAccountNumber)
                .amount(amount)
                .build();

            AccountBalanceResponseDTO responseDTO = AccountBalanceResponseDTO.builder()
                .accountNumber(testAccountNumber)
                .amount(amount)
                .balance(new BigDecimal("150000"))
                .build();

            when(accountService.deposit(eq(testAccountNumber), eq(amount))).thenReturn(responseDTO);

            performBalanceRequest(Endpoint.DEPOSIT, requestDTO)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseMessage.DEPOSIT_SUCCESSFUL.getMessage()))
                .andExpect(jsonPath("$.data.accountNumber").value(testAccountNumber))
                .andExpect(jsonPath("$.data.amount").value(50000))
                .andExpect(jsonPath("$.data.balance").value(150000));

            verify(accountService).deposit(testAccountNumber, amount);
        }

        /**
         * 계좌 출금 성공
         */
        @Test
        void withdraw_success() throws Exception {
            BigDecimal amount = new BigDecimal("30000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(testAccountNumber)
                .amount(amount)
                .build();

            AccountBalanceResponseDTO responseDTO = AccountBalanceResponseDTO.builder()
                .accountNumber(testAccountNumber)
                .amount(amount)
                .balance(new BigDecimal("70000"))
                .build();

            when(accountService.withdraw(eq(testAccountNumber), eq(amount))).thenReturn(responseDTO);

            performBalanceRequest(Endpoint.WITHDRAW, requestDTO)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseMessage.WITHDRAW_SUCCESSFUL.getMessage()))
                .andExpect(jsonPath("$.data.accountNumber").value(testAccountNumber))
                .andExpect(jsonPath("$.data.amount").value(30000))
                .andExpect(jsonPath("$.data.balance").value(70000));

            verify(accountService).withdraw(testAccountNumber, amount);
        }

        /**
         * 계좌 입금 시 계좌가 존재하지 않는 경우
         */
        @Test
        void deposit_accountNotFound() throws Exception {
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber("nonexistent")
                .amount(new BigDecimal("50000"))
                .build();

            expectBalanceError(Endpoint.DEPOSIT, requestDTO, ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        /**
         * 계좌 출금 시 잔액이 부족한 경우
         */
        @Test
        void withdraw_insufficientBalance() throws Exception {
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(testAccountNumber)
                .amount(new BigDecimal("1000000"))
                .build();

            expectBalanceError(Endpoint.WITHDRAW, requestDTO, ErrorCode.INSUFFICIENT_BALANCE, HttpStatus.BAD_REQUEST);
        }
    }
}