package ru.taranenkoant.banking.account.web;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.taranenkoant.banking.account.service.AccountService;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.request.AccountStatusUpdateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 24.04.2026
 */

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }

    @PostMapping("/{accountNumber}/status")
    public AccountResponse updateStatus(@PathVariable String accountNumber,
                                        @Valid @RequestBody AccountStatusUpdateRequest request) {
        return accountService.updateStatus(accountNumber, request.getStatus());
    }
}
