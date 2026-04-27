package ru.taranenkoant.banking.account.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taranenkoant.banking.account.domain.Account;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;
import ru.taranenkoant.banking.account.exception.AccountBlockedException;
import ru.taranenkoant.banking.account.exception.AccountNotFoundException;
import ru.taranenkoant.banking.account.exception.InsufficientFundsException;
import ru.taranenkoant.banking.account.mapper.AccountMapper;
import ru.taranenkoant.banking.account.repository.AccountRepository;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Account number already exist: " + request.getAccountNumber());
        }

        Account account = accountMapper.toEntity(request);
        Account saved = accountRepository.save(account);
        log.info("Created new account: {}", saved.getAccountNumber());
        return accountMapper.toResponse(saved);
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = findAccountByNumber(accountNumber);
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateStatus(String accountNumber, AccountStatus newStatus) {
        Account account = findAccountByNumber(accountNumber);
        account.setStatus(newStatus);
        Account updated = accountRepository.save(account);
        log.info("Account {} status changet to {}", accountNumber, newStatus);
        return accountMapper.toResponse(updated);
    }

    @Transactional
    public void deposit(String accountNumber, BigDecimal amount) {
        Account accountByNumber = findAccountByNumber(accountNumber);
        validateAccountActive(accountByNumber);
        accountByNumber.setBalance(accountByNumber.getBalance().add(amount));
        accountRepository.save(accountByNumber);
        log.info("Deposited {} to account {}", amount, accountNumber);
    }

    @Transactional
    public void withdraw(String accountNumber, BigDecimal amount) {
        Account account = findAccountByNumber(accountNumber);
        validateAccountActive(account);
        BigDecimal currentBalance = account.getBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, amount, currentBalance);
        }
        account.setBalance(currentBalance.subtract(amount));
        accountRepository.save(account);
        log.info("Withdraw {} from account {}", amount, accountNumber);
    }

    private Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException(account.getAccountNumber());
        }
    }
}
