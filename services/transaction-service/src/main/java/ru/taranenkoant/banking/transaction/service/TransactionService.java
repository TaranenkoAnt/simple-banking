package ru.taranenkoant.banking.transaction.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.taranenkoant.banking.transaction.client.AccountClient;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;
import ru.taranenkoant.banking.transaction.domain.Transaction;
import ru.taranenkoant.banking.transaction.domain.TransactionStatus;
import ru.taranenkoant.banking.transaction.dto.TransactionRequest;
import ru.taranenkoant.banking.transaction.dto.TransactionResponse;
import ru.taranenkoant.banking.transaction.mapper.TransactionMapper;
import ru.taranenkoant.banking.transaction.repository.OutboxEventRepository;
import ru.taranenkoant.banking.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountClient accountClient;
    private final TransactionMapper mapper;

    @Transactional
    public TransactionResponse processTransfer(TransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected, returning existing transaction {}", existing.get().getId());
            return mapper.toResponse(existing.get());
        }

        Transaction transaction = Transaction.builder()
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            log.debug("Checking accounts {} and {}", request.getFromAccount(), request.getToAccount());

            var fromAcc = accountClient.getAccount(request.getFromAccount());
            var toAcc = accountClient.getAccount(request.getToAccount());
            if (!"ACTIVE".equals(fromAcc.getStatus()) || !"ACTIVE".equals(toAcc.getStatus())) {
                throw new IllegalStateException("One or both accounts are not ACTIVE");
            }

            if (fromAcc.getBalance().compareTo(request.getAmount()) < 0) {
                throw new IllegalStateException("Insufficient funds on account " + request.getFromAccount());
            }

            accountClient.withDraw(request.getFromAccount(), request.getAmount());
            try {
                accountClient.deposit(request.getToAccount(), request.getAmount());
            } catch (Exception depositEx) {
                log.error("Deposit failed, compensating withdrawal for {}", request.getFromAccount());
                accountClient.deposit(request.getFromAccount(), request.getAmount());
                throw new RuntimeException("Transfer failed during deposit", depositEx);
            }

            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(savedTransaction);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Transaction")
                    .aggregateId(savedTransaction.getId().toString())
                    .eventType("TransactionCompleted")
                    .payload(mapper.toJson(savedTransaction))
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(event);
            log.info("Transfer completed successfully: id={}", savedTransaction.getId());
            return mapper.toResponse(savedTransaction);
        } catch (Exception e) {
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            log.error("Transfer failed: {}", e.getMessage());
            markTransactionFailed(transaction);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTransactionFailed(Transaction transaction) {
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }
}