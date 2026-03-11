package com.transfer.system.service;

import com.transfer.system.account.AccountTestApplication;
import com.transfer.system.domain.AccountNumberEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(AccountNumberGeneratorService.class)
@ContextConfiguration(classes = AccountTestApplication.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountNumberGeneratorServiceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AccountNumberGeneratorService accountNumberGeneratorService;

    private static final String ACCOUNT_PREFIX = "001";

    // ========================== 계좌 번호 생성 테스트 =========================
    @Nested
    class GenerateAccountNumberTest {

        /**
         * 시퀀스가 이미 존재할 경우
         */
        @Test
        void generateAccountNumber_whenSequenceExists() {
            LocalDate today = LocalDate.now();
            AccountNumberEntity existingSequence = new AccountNumberEntity(today, 1L);
            testEntityManager.persistAndFlush(existingSequence);

            String newAccountNumber = accountNumberGeneratorService.generateAccountNumber();

            String datePart = today.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String expectedAccountNumber = ACCOUNT_PREFIX + datePart + "00002";
            assertNotNull(newAccountNumber);
            assertEquals(expectedAccountNumber, newAccountNumber);

            AccountNumberEntity updatedSequence = testEntityManager.find(AccountNumberEntity.class, today);
            assertEquals(2L, updatedSequence.getLastNumber());
        }

        /**
         * 오늘 날짜의 시퀀스가 없을 경우
         */
        @Test
        void generateAccountNumber_whenSequenceIsNew() {
            LocalDate today = LocalDate.now();

            String newAccountNumber = accountNumberGeneratorService.generateAccountNumber();

            String datePart = today.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String expectedAccountNumber = ACCOUNT_PREFIX + datePart + "00001";
            assertNotNull(newAccountNumber);
            assertEquals(expectedAccountNumber, newAccountNumber);

            AccountNumberEntity createdSequence = testEntityManager.find(AccountNumberEntity.class, today);
            assertNotNull(createdSequence);
            assertEquals(1L, createdSequence.getLastNumber());
        }
    }
}