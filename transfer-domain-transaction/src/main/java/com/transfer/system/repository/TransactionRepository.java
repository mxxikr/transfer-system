package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query(
        value = """
            SELECT te
            FROM TransactionEntity te
            LEFT JOIN FETCH te.fromAccount fa
            LEFT JOIN FETCH te.toAccount ta
            WHERE te.fromAccount = :account OR te.toAccount = :account
            ORDER BY te.createdTimeStamp DESC
        """,
        countQuery = """
            SELECT COUNT(te)
            FROM TransactionEntity te
            WHERE te.fromAccount = :account OR te.toAccount = :account
        """
    )
    Page<TransactionEntity> findAllByAccount(@Param("account") AccountEntity account, Pageable pageable); // 특정 계좌의 모든 거래 내역 조회

    // 일일 한도 계산용 조회
    @Query("""
        SELECT COALESCE(SUM(te.amount), 0)
        FROM TransactionEntity te
        WHERE te.fromAccount.accountNumber = :accountNumber
          AND te.transactionType = :type
          AND te.createdTimeStamp BETWEEN :startTime AND :endTime
    """)
    BigDecimal getSumTodayUsedAmount(@Param("accountNumber") String accountNumber, @Param("type") TransactionType type, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 계좌 삭제 전 거래 존재 여부
    @Query("""
        SELECT CASE WHEN COUNT(te) > 0 THEN true ELSE false END
        FROM TransactionEntity te
        WHERE te.fromAccount = :accountEntity OR te.toAccount = :accountEntity
    """)
    boolean existsByFromOrTo(@Param("accountEntity") AccountEntity accountEntity);
}