package ru.taranenkoant.banking.account.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.taranenkoant.banking.account.domain.AccountStatus;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */

@Data
public class AccountStatusUpdateRequest {

    @NotNull
    private AccountStatus status;
}
