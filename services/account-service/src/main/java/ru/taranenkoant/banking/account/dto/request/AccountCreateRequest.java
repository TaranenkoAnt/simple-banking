package ru.taranenkoant.banking.account.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
@Data
public class AccountCreateRequest {

    @NotBlank
    @Size(min = 10, max = 20)
    private String accountNumber;
    @NotBlank
    @Size(max = 100)
    private String ownerName;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal initialBalance;
    @NotNull private Currency currency;
    private AccountStatus status;
}
