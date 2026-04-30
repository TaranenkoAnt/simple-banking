package ru.taranenkoant.banking.transaction.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;
import ru.taranenkoant.banking.transaction.domain.Transaction;
import ru.taranenkoant.banking.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TransactionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveTransaction() {
        var transaction = Transaction.builder()
                .fromAccount("111")
                .toAccount("222")
                .amount(new BigDecimal("100.00"))
                .fee(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("key1")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByIdempotencyKey() {
        Transaction transaction = Transaction.builder()
                .fromAccount("111")
                .toAccount("222")
                .amount(new BigDecimal("100.00"))
                .fee(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("unique-key")
                .build();
        transactionRepository.save(transaction);

        Optional<Transaction> found = transactionRepository.findByIdempotencyKey("unique-key");
        assertThat(found).isPresent();
    }

    @Test
    void shouldSaveOutboxEventAndRetrieveUnpublished() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Transaction")
                .aggregateId("1")
                .eventType("Completed")
                .payload("{\"id\":1}")
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventRepository.save(event);

        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();
        assertThat(unpublished).hasSize(1);
        assertThat(unpublished.get(0).isPublished()).isFalse();
    }

    @Test
    void shouldEnforceUniqueIdempotencyKey() {
        Transaction t1 = Transaction.builder()
                .fromAccount("111").toAccount("222")
                .amount(new BigDecimal("100")).fee(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("dup-key").build();
        transactionRepository.save(t1);

        Transaction t2 = Transaction.builder()
                .fromAccount("333").toAccount("444")
                .amount(new BigDecimal("200")).fee(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("dup-key").build();

        assertThrows(Exception.class, () -> {
            transactionRepository.save(t2);
            entityManager.flush();
        });
    }
}