package ru.taranenkoant.banking.account.exception;


/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
