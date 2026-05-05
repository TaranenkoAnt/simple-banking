package ru.taranenkoant.banking.transaction.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 04.05.2026
 */

@Data
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createAt;
    private Integer version;
}
