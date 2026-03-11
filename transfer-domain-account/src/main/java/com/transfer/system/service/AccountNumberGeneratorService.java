package com.transfer.system.service;

import com.transfer.system.domain.AccountNumberEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.transfer.system.utils.TimeUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountNumberGeneratorService {

    private final EntityManager entityManager;

    private static final String ACCOUNT_PREFIX = "001";
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    @Transactional
    public String generateAccountNumber() {
        LocalDate today = nowKstLocalDate();
        // 날짜 기준으로 시퀀스 엔티티를 가져오거나 생성
        AccountNumberEntity seq = entityManager.find(AccountNumberEntity.class, today, LockModeType.PESSIMISTIC_WRITE);

        if (seq == null) {
            seq = new AccountNumberEntity();
            seq.setId(today);
            seq.setLastNumber(1L);
            entityManager.persist(seq);
            log.debug("[AccountNumber] 신규 시퀀스 생성 date: {}, lastNumber: {}", today, seq.getLastNumber());
        } else {
            seq.setLastNumber(seq.getLastNumber() + 1);
            log.debug("[AccountNumber] 시퀀스 증가 date: {}, lastNumber: {}", today, seq.getLastNumber());
        }

        String accountNumber = format(today, seq.getLastNumber());
        log.debug("[AccountNumber] 생성된 계좌 번호: {}", accountNumber);

        return accountNumber;
    }

    private String format(LocalDate date, Long value) {
        String datePart = date.format(YYMMDD); // 6자리 날짜
        String sequencePart = String.format("%05d", value); // 5자리 시퀀스
        return ACCOUNT_PREFIX + datePart + sequencePart; // 001YYMMDD00001
    }
}