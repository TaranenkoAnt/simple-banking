package ru.taranenkoant.banking.account.exception;


/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(String accountNumber) {
        super("Account " + accountNumber + " is blocked");
    }
}
