package com.transfer.system.repository;

import com.transfer.system.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>, TransactionRepositoryCustom {
}
