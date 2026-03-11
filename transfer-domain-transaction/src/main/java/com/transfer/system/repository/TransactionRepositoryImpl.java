package com.transfer.system.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.QAccountEntity;
import com.transfer.system.domain.QTransactionEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 계좌의 거래 내역을 Fetch Join을 사용하여 페이징 조회
     */
    @Override
    public Page<TransactionEntity> findAllByAccountWithFetchJoin(AccountEntity account, Pageable pageable) {
        QTransactionEntity transaction = QTransactionEntity.transactionEntity;
        QAccountEntity fromAccount = new QAccountEntity("fromAccount");
        QAccountEntity toAccount = new QAccountEntity("toAccount");

        JPAQuery<TransactionEntity> query = queryFactory
                .selectFrom(transaction)
                .leftJoin(transaction.fromAccount, fromAccount).fetchJoin()
                .leftJoin(transaction.toAccount, toAccount).fetchJoin()
                .where(transaction.fromAccount.eq(account).or(transaction.toAccount.eq(account)));

        // 페이징 및 정렬 적용
        for (Sort.Order o : pageable.getSort()) {
            PathBuilder<TransactionEntity> pathBuilder = new PathBuilder<>(transaction.getType(), transaction.getMetadata());
            query.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        List<TransactionEntity> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(transaction.count())
                .from(transaction)
                .where(transaction.fromAccount.eq(account).or(transaction.toAccount.eq(account)));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 계좌의 특정 거래 유형에 대한 금일 사용 금액 합계 조회
     */
    @Override
    public BigDecimal getSumTodayUsedAmount(String accountNumber, TransactionType type, LocalDateTime startTime, LocalDateTime endTime) {
        QTransactionEntity transaction = QTransactionEntity.transactionEntity;

        BigDecimal sum = queryFactory
                .select(transaction.amount.sum())
                .from(transaction)
                .where(transaction.fromAccount.accountNumber.eq(accountNumber)
                        .and(transaction.transactionType.eq(type))
                        .and(transaction.createdTimeStamp.between(startTime, endTime)))
                .fetchOne();

        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * 특정 계좌와 연관된 거래 내역 존재 여부 확인
     */
    @Override
    public boolean existsByFromOrTo(AccountEntity accountEntity) {
        QTransactionEntity transaction = QTransactionEntity.transactionEntity;

        Integer fetchOne = queryFactory
                .selectOne()
                .from(transaction)
                .where(transaction.fromAccount.eq(accountEntity).or(transaction.toAccount.eq(accountEntity)))
                .fetchFirst();

        return fetchOne != null;
    }
}
