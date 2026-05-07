package ru.taranenkoant.banking.transaction.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */

@Data
@Builder
public class TransactionResponse {
   private Long id;
   private String fromAccount;
   private String toAccount;
   private BigDecimal amount;
   private BigDecimal fee;
   private String status;
   private LocalDateTime createdAt;
}
