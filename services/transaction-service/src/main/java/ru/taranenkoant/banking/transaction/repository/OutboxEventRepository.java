package ru.taranenkoant.banking.transaction.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;

import java.util.List;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 29.04.2026
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByPublishedFalse();
}
