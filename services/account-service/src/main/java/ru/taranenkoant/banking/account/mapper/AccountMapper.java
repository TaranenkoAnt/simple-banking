package ru.taranenkoant.banking.account.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.taranenkoant.banking.account.domain.Account;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 23.04.2026
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "balance", source = "initialBalance")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Account toEntity(AccountCreateRequest request);

    AccountResponse toResponse(Account account);
}
