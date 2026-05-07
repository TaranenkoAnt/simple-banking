package ru.taranenkoant.banking.transaction.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.taranenkoant.banking.transaction.domain.Transaction;
import ru.taranenkoant.banking.transaction.dto.TransactionResponse;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    TransactionResponse toResponse(Transaction transaction);

    default String toJson(Transaction transaction) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            return objectMapper.writeValueAsString(transaction);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize transaction", e);
        }
    }
}
