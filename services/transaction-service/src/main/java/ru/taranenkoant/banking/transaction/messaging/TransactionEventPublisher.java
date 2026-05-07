package ru.taranenkoant.banking.transaction.messaging;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;
import ru.taranenkoant.banking.transaction.repository.OutboxEventRepository;

import java.util.List;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventPublisher {
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send("transaction-completed", event.getPayload()).get();
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Published event {} to Kafka", event.getId());
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
