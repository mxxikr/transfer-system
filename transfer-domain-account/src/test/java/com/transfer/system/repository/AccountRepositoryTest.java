package com.transfer.system.repository;

import com.transfer.system.account.AccountTestApplication;
import com.transfer.system.config.QueryDslConfig;
import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AccountTestApplication.class)
@Import(QueryDslConfig.class)
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private static final String testFromAccountNumber = "00125080800001";
    private static final String testToAccountNumber = "00125080800002";

    /**
     * 테스트용 계좌 생성 및 저장
     */
    private AccountEntity createTestAccount(String accountNumber, String accountName, BigDecimal balance) {
        return AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(balance)
            .accountStatus(AccountStatus.ACTIVE)
            .build();
    }

    /**
     * 특정 상태의 계좌
     */
    private AccountEntity createTestAccountWithStatus(String accountNumber, String accountName, BigDecimal balance, AccountStatus status) {
        return AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(balance)
            .accountStatus(status)
            .build();
    }

    /**
     * 특정 통화의 계좌
     */
    private AccountEntity createTestAccountWithCurrency(String accountNumber, String accountName, BigDecimal balance, CurrencyType currency) {
        return AccountEntity.builder()
                .accountNumber(accountNumber)
                .accountName(accountName)
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(currency)
                .balance(balance)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    /**
     * 계좌를 데이터베이스에 저장
     */
    private AccountEntity saveAccount(String accountNumber, String accountName, BigDecimal balance) {
        AccountEntity accountEntity = createTestAccount(accountNumber, accountName, balance);
        return entityManager.persistAndFlush(accountEntity);
    }

    // ==================== Repository 메서드 테스트 ====================
    @Nested
    class AccountRepositoryMethodTest {

        /**
         * 계좌 저장 성공
         */
        @Test
        void save_success() {
            AccountEntity accountEntity = createTestAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            AccountEntity savedAccount = accountRepository.save(accountEntity);

            assertThat(savedAccount).isNotNull();
            assertThat(savedAccount.getAccountId()).isNotNull();
            assertThat(savedAccount.getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(savedAccount.getCreatedTimeStamp()).isNotNull();
        }

        /**
         * ID로 계좌 조회 성공
         */
        @Test
        void findById_success() {
            AccountEntity accountEntity = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            UUID accountId = accountEntity.getAccountId();

            Optional<AccountEntity> foundAccount = accountRepository.findById(accountId);

            assertThat(foundAccount).isPresent();
        }

        /**
         * 존재하지 않는 ID로 조회 시 빈 Optional 반환
         */
        @Test
        void findById_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            Optional<AccountEntity> foundAccount = accountRepository.findById(nonExistentId);

            assertThat(foundAccount).isEmpty();
        }

        /**
         * 계좌 삭제 성공 테스트
         */
        @Test
        void delete_success() {
            AccountEntity savedAccount = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            UUID accountId = savedAccount.getAccountId();

            accountRepository.delete(savedAccount);
            entityManager.flush();

            Optional<AccountEntity> deletedAccount = accountRepository.findById(accountId);
            assertThat(deletedAccount).isEmpty();
        }

        /**
         * 여러 개의 계좌 전체 조회 테스트
         */
        @ParameterizedTest
        @MethodSource("multipleAccountsProvider")
        void findAll_multipleAccounts(int accountCount) {
            for (int i = 1; i <= accountCount; i++) {
                saveAccount("001" + i + "111111111", "test" + i, new BigDecimal("100000"));
            }

            List<AccountEntity> accounts = accountRepository.findAll();

            assertThat(accounts).hasSize(accountCount);
        }

        /**
         * 여러 계좌 개수 테스트 데이터 제공
         */
        private static Stream<Arguments> multipleAccountsProvider() {
            return Stream.of(
                Arguments.of(1),
                Arguments.of(3),
                Arguments.of(5)
            );
        }
    }

    // ==================== 계좌 조회 테스트 ====================
    @Nested
    class FindByAccountNumberTest {

        /**
         * 계좌번호로 계좌 조회 성공
         */
        @Test
        void findByAccountNumber_success() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumber(testFromAccountNumber);

            assertThat(foundAccount).isPresent();
        }

        /**
         * 계좌번호로 계좌 비관적 락 조회 성공
         */
        @Test
        void findByAccountNumberLock_success() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumberLock(testFromAccountNumber);

            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getAccountNumber()).isEqualTo(testFromAccountNumber);
        }
    }

    // ==================== 계좌 잔액 업데이트 ====================
    @Nested
    class BalanceUpdateTest {

        /**
         * 계좌 잔액 업데이트 성공
         */
        @Test
        void updateBalance_success() {
            AccountEntity account = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            BigDecimal newBalance = new BigDecimal("150000");

            account.updateBalance(newBalance);
            entityManager.flush(); // Auditing 반영 유도

            AccountEntity updatedAccount = accountRepository.findById(account.getAccountId()).get();

            assertThat(updatedAccount.getBalance()).isEqualByComparingTo(newBalance);
            assertThat(updatedAccount.getUpdatedTimeStamp()).isNotNull();
        }
    }
}
