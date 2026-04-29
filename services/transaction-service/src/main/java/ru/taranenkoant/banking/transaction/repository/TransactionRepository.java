package ru.taranenkoant.banking.transaction.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.taranenkoant.banking.transaction.domain.Transaction;

import java.util.Optional;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 29.04.2026
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
