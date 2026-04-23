package ru.taranenkoant.banking.account.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.taranenkoant.banking.account.domain.Account;

import java.util.Optional;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber (String accountNumber);
}
