package com.transfer.system.service;

import com.transfer.system.account.AccountTestApplication;
import com.transfer.system.config.QueryDslConfig;
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
@Import({AccountNumberGeneratorService.class, QueryDslConfig.class})
@ContextConfiguration(classes = AccountTestApplication.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountNumberGeneratorServiceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AccountNumberGeneratorService accountNumberGeneratorService;

    @Nested
    class GenerateAccountNumberTest {

        @Test
        void generateAccountNumber_whenSequenceIsNew() {
            String accountNumber = accountNumberGeneratorService.generateAccountNumber();

            LocalDate today = LocalDate.now();
            String datePart = today.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String expected = "001" + datePart + "00001";
            
            assertNotNull(accountNumber);
            assertEquals(14, accountNumber.length());
            assertEquals(expected, accountNumber);

            AccountNumberEntity createdSequence = testEntityManager.find(AccountNumberEntity.class, today);
            assertNotNull(createdSequence);
            assertEquals(1L, createdSequence.getLastNumber());
        }

        @Test
        void generateAccountNumber_whenSequenceExists() {
            LocalDate today = LocalDate.now();
            AccountNumberEntity existingSequence = new AccountNumberEntity(today, 5L);
            testEntityManager.persistAndFlush(existingSequence);

            String accountNumber = accountNumberGeneratorService.generateAccountNumber();

            String datePart = today.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String expected = "001" + datePart + "00006";
            assertEquals(expected, accountNumber);

            AccountNumberEntity updatedSequence = testEntityManager.find(AccountNumberEntity.class, today);
            assertEquals(6L, updatedSequence.getLastNumber());
        }
    }
}
