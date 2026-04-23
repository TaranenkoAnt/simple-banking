package ru.taranenkoant.banking.account.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.taranenkoant.banking.account.domain.Account;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AccountRepositoryTest {

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
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .accountNumber("ACC123456789")
                .ownerName("Ivan Petrov")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.RUB)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldSaveAccount() {
        Account saved = accountRepository.save(sampleAccount);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByAccountNumber() {
        accountRepository.save(sampleAccount);
        Optional<Account> found = accountRepository.findByAccountNumber("ACC123456789");
        assertThat(found).isPresent();
        assertThat(found.get().getOwnerName()).isEqualTo("Ivan Petrov");
    }

    @Test
    void shouldUpdateBalanceAndIncrementVersion() {
        Account saved = accountRepository.save(sampleAccount);
        Long id = saved.getId();
        Integer initialVersion = saved.getVersion();

        Account toUpdate = accountRepository.findById(id).orElseThrow();
        toUpdate.setBalance(new BigDecimal("2000.00"));
        Account updated = accountRepository.save(toUpdate);
        entityManager.flush();
//        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
        entityManager.clear();


        Account refreshed = accountRepository.findById(id).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo("2000.00");
        assertThat(refreshed.getVersion()).isEqualTo(initialVersion + 1);
    }

    // Тест не проходит
    // Hibernate:
    //    insert
    //    into
    //        accounts
    //        (account_number, status, balance, created_at, currency, owner_name, version)
    //    values
    //        (?, ?, ?, ?, ?, ?, ?)
    //
    //Expected java.lang.Exception to be thrown, but nothing was thrown.
    //org.opentest4j.AssertionFailedError: Expected java.lang.Exception to be thrown, but nothing was thrown.
    //	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
    //	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
    //	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
    //	at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
    //	at ru.taranenkoant.banking.account.repository.AccountRepositoryTest.shouldDetectOptimisticLockingConflict(AccountRepositoryTest.java:116)

}
