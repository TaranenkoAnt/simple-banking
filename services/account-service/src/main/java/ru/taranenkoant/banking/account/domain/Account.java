package ru.taranenkoant.banking.account.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccountStatus status;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Version
    private Integer version;
}
