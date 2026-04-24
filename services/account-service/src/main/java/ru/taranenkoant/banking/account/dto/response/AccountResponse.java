package ru.taranenkoant.banking.account.dto.response;


import lombok.Builder;
import lombok.Data;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private Currency currency;
    private AccountStatus status;
    private LocalDateTime createAt;
    private Integer version;
}
