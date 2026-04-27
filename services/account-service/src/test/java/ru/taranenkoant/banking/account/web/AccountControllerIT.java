package ru.taranenkoant.banking.account.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.request.AccountStatusUpdateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountControllerIT {

    private final String ACCOUNT_NUMBER_I = "1234567890";
    private final String ACCOUNT_NUMBER_S = "1234567891";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("integration_test_db")
            .withUsername("sa")
            .withPassword("sa");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/accounts";
    }

    @Test
    void shouldCreateAndGetAccount() {
        AccountCreateRequest request = new AccountCreateRequest();
        request.setAccountNumber(ACCOUNT_NUMBER_I);
        request.setOwnerName("Integration user");
        request.setInitialBalance(new BigDecimal("500.00"));
        request.setCurrency(Currency.USD);

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(baseUrl, request, AccountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AccountResponse created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER_I);
        assertThat(created.getBalance()).isEqualByComparingTo("500.00");

        ResponseEntity<AccountResponse> getResponse = restTemplate.getForEntity(baseUrl + "/" + ACCOUNT_NUMBER_I, AccountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getOwnerName()).isEqualTo("Integration user");
    }

    @Test
    void shouldUpdateStatus() {
        AccountCreateRequest createRequest = new AccountCreateRequest();
        createRequest.setAccountNumber(ACCOUNT_NUMBER_S);
        createRequest.setOwnerName("Status user");
        createRequest.setInitialBalance(new BigDecimal("200.00"));
        createRequest.setCurrency(Currency.EUR);

        restTemplate.postForEntity(baseUrl, createRequest, AccountResponse.class);

        AccountStatusUpdateRequest statusRequest = new AccountStatusUpdateRequest();
        statusRequest.setStatus(AccountStatus.BLOCKED);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AccountStatusUpdateRequest> requestEntity = new HttpEntity<>(statusRequest, headers);

        ResponseEntity<AccountResponse> patchResponse = restTemplate.exchange(
                baseUrl + "/" + ACCOUNT_NUMBER_S + "/status",
                HttpMethod.POST,
                requestEntity,
                AccountResponse.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void shouldReturnNotFoundForMissingAccount() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/NONEXIST", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message").toString()).contains("NONEXIST");
    }

    @Test
    void shouldReturnBadRequestOnInvalidCreate() {
        AccountCreateRequest badRequest = new AccountCreateRequest();
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, badRequest, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}