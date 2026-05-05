package ru.taranenkoant.banking.transaction.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;
import ru.taranenkoant.banking.transaction.dto.AccountResponse;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class AccountClientTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private AccountClient accountClient;
    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void setBaseUrl(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        registry.add("account-service.base-url", wireMockServer::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetState() {
        // Сброс Circuit Breaker
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.circuitBreaker("accountService").reset();
        }
        // Очистка всех заглушек и сценариев WireMock
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }


    @Test
    void shouldGetAccountSuccessfully() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/12345"))
                .willReturn(aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("{\"id\":1,\"accountNumber\":\"12345\",\"ownerName\":\"Test\",\"balance\":100.00,\"currency\":\"RUB\",\"status\":\"ACTIVE\",\"version\":0}")
                )
        );


        AccountResponse response = accountClient.getAccount("12345");
        assertThat(response.getAccountNumber()).isEqualTo("12345");
        assertThat(response.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRetryOnServerError() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/error"))
                .inScenario("Retry")
                .willSetStateTo("Attempt1")
                .willReturn(aResponse().withStatus(500))
        );

        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/error"))
                .inScenario("Retry")
                .whenScenarioStateIs("Attempt1")
                .willSetStateTo("Attempt2")
                .willReturn(aResponse().withStatus(500))
        );

        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/error"))
                .inScenario("Retry")
                .whenScenarioStateIs("Attempt2")

                .willReturn(aResponse()
                        .withHeader("Content-type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\":2,\"accountNumber\":\"error\",\"balance\":0,\"status\":\"ACTIVE\",\"version\":0}"))
        );

        AccountResponse response = accountClient.getAccount("error");
        assertThat(response).isNotNull();
        wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/api/v1/accounts/error")));
    }

    @Test
    void shouldThrowWhenCircuitBreakerOpen() {
        // 1. Создаём заглушку на 500 (один раз)
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/breakerTest"))
                .willReturn(aResponse().withStatus(500)));

        // 2. Вызываем метод 5 раз, чтобы набрать статистику (breaker ещё closed)
        for (int i = 0; i < 5; i++) {
            try {
                accountClient.getAccount("breakerTest");
            } catch (Exception ignored) {
                // ожидаем исключения на 500
            }
        }

        // 3. Шестой вызов должен быть уже разомкнут и сразу упасть с CallNotPermittedException
        //    (или с вашим fallback RuntimeException)
        assertThatThrownBy(() -> accountClient.getAccount("breakerTest"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account service unavailable");

        // 4. Проверяем, что WireMock получил не более 5 запросов (последний даже не дошёл)
        wireMockServer.verify(5, getRequestedFor(urlPathEqualTo("/api/v1/accounts/breakerTest")));
    }
}