package ru.taranenkoant.banking.transaction.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */

@Data
public class TransactionRequest {
    @NotBlank
    private String fromAccount;
    @NotBlank
    private String toAccount;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotBlank
    private String idempotencyKey;
}
