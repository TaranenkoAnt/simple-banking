package ru.taranenkoant.banking.account.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@Data
public class DepositRequest {
    @NotBlank
    private String accountNumber;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
}
