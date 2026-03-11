package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.utils.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountNumberGeneratorService accountNumberGeneratorService;

    private static final String BANK_NAME = "mxxikrBank";

    /***
     * 계좌 생성
     */
    @Override
    @Transactional
    public AccountEntity createAccount(String accountName, AccountType accountType, CurrencyType currencyType) {
        String accountNumber = accountNumberGeneratorService.generateAccountNumber();

        log.debug("[AccountService] 생성된 계좌번호: {}", accountNumber);

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            log.warn("[AccountService] 중복 계좌 번호 감지: {}", accountNumber);
            throw new TransferSystemException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }

        AccountEntity accountEntity = AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName(BANK_NAME)
            .accountType(accountType)
            .currencyType(currencyType)
            .balance(MoneyUtils.normalize(BigDecimal.ZERO))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        AccountEntity savedAccountEntity = accountRepository.save(accountEntity);
        log.debug("[AccountService] 계좌 생성 완료 id: {}, number: {}", savedAccountEntity.getAccountId(), savedAccountEntity.getAccountNumber());

        return savedAccountEntity;
    }

    /**
     * 계좌 조회
     */
    @Override
    public AccountEntity getAccount(UUID id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    /**
     * 계좌 삭제
     */
    @Override
    public void deleteAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.findById(id)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 거래 내역이 있고 계좌 상태가 ACTIVE인 경우 삭제 불가
        if (accountEntity.getAccountStatus() == AccountStatus.ACTIVE) {
            log.warn("[AccountService] 활성 계좌 삭제 불가 accountId: {}, satus: {}", id, accountEntity.getAccountStatus());
            throw new TransferSystemException(ErrorCode.ACCOUNT_CANNOT_BE_DELETED);
        }

        // 거래 없으면 실제 삭제 허용
        accountRepository.delete(accountEntity);
    }
}