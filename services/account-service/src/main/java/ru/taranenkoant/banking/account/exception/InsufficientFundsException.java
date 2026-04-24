package ru.taranenkoant.banking.account.exception;


import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountNumber, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient funds on account %s: requested %s, available %s",
                accountNumber, requested, available));
    }
}