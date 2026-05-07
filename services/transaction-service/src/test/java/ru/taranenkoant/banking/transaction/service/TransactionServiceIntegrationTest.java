package ru.taranenkoant.banking.transaction.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;
import ru.taranenkoant.banking.transaction.dto.TransactionRequest;
import ru.taranenkoant.banking.transaction.dto.TransactionResponse;
import ru.taranenkoant.banking.transaction.repository.OutboxEventRepository;
import ru.taranenkoant.banking.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    static WireMockServer wireMockServer = new WireMockServer(0);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.admin.fail-fast", () -> "false");
        registry.add("spring.kafka.listener.missing-topics-fatal", () -> "false");
        registry.add("account-service.base-url", () -> wireMockServer.baseUrl());
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();   // Исправлено: было start()
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        transactionRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldCreateTransactionAndPublishOutboxEvent() {
        // Stub Account Service
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/1111"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"accountNumber\":\"1111\",\"balance\":500.00,\"status\":\"ACTIVE\"}")));
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/accounts/2222"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"accountNumber\":\"2222\",\"balance\":100.00,\"status\":\"ACTIVE\"}")));
        wireMockServer.stubFor(post(urlPathEqualTo("/api/v1/accounts/1111/withdraw"))
                .willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(post(urlPathEqualTo("/api/v1/accounts/2222/deposit"))
                .willReturn(aResponse().withStatus(200)));

        TransactionRequest request = new TransactionRequest();
        request.setFromAccount("1111");
        request.setToAccount("2222");
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("idem-" + System.currentTimeMillis());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/transactions",
                entity,
                TransactionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        TransactionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("COMPLETED");

        // Проверка сохранения транзакции
        assertThat(transactionRepository.count()).isEqualTo(1);

        // Проверка Outbox события
        List<OutboxEvent> events = outboxEventRepository.findByPublishedFalse();
        assertThat(events).hasSize(1);

        // Ожидание публикации в Kafka (планировщик каждые 5с)
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty()
        );
    }
}