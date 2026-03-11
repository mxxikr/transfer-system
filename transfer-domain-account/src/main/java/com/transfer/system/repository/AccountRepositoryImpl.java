package com.transfer.system.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.QAccountEntity;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 계좌 번호로 계좌를 비관적 락(PESSIMISTIC_WRITE)과 함께 조회
     */
    @Override
    public Optional<AccountEntity> findByAccountNumberLock(String accountNumber) {
        QAccountEntity account = QAccountEntity.accountEntity;

        return Optional.ofNullable(queryFactory
                .selectFrom(account)
                .where(account.accountNumber.eq(accountNumber))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne());
    }
}
