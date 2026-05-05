package ru.taranenkoant.banking.transaction.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 04.05.2026
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {
    private String accountNumber;
    private BigDecimal amount;
}
