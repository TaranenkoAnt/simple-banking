package ru.taranenkoant.banking.transaction.client;


import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.taranenkoant.banking.transaction.dto.AccountResponse;
import ru.taranenkoant.banking.transaction.dto.DepositRequest;
import ru.taranenkoant.banking.transaction.dto.WithdrawRequest;

import java.math.BigDecimal;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 04.05.2026
 */
@Service
@Slf4j
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder restClientBuilder,
                         @Value("${account-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Retry(name = "accountService", fallbackMethod = "fallbackGetAccount")
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackGetAccount")
    public AccountResponse getAccount(String accountNumber) {
        log.debug("Getting account {}", accountNumber);
        return restClient.get()
                .uri("/api/v1/accounts/{accountNumber}", accountNumber)
                .retrieve()
                .body(AccountResponse.class);
    }

    @Retry(name = "accountService")
    @CircuitBreaker(name = "accountService")
    public void deposit(String accountNumber, BigDecimal amount) {
        log.debug("Depositing {} to account {}", amount, accountNumber);
        restClient.post()
                .uri("/api/v1/accounts/{accountNumber}/deposit", accountNumber)
                .body(new DepositRequest(accountNumber, amount))
                .retrieve()
                .toBodilessEntity();
    }

    @Retry(name = "accountService")
    @CircuitBreaker(name = "accountService")
    public void withDraw(String accountNumber, BigDecimal amount) {
        log.debug("Withdrawing  {} from account {}", amount, accountNumber);
        restClient.post()
                .uri("/api/v1/accounts/{accountNumber}/withdraw", accountNumber)
                .body(new WithdrawRequest(accountNumber, amount))
                .retrieve()
                .toBodilessEntity();
    }

    public AccountResponse fallbackGetAccount(String accountNumber, Throwable t) {
        log.error("Failed to get account {}, reason: {}", accountNumber, t.getMessage());
        throw new RuntimeException("Account service unavailable");
    }
}