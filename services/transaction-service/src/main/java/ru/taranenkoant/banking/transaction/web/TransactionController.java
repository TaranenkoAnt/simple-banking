package ru.taranenkoant.banking.transaction.web;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.taranenkoant.banking.transaction.dto.TransactionRequest;
import ru.taranenkoant.banking.transaction.dto.TransactionResponse;
import ru.taranenkoant.banking.transaction.service.TransactionService;

/**
 * {@code @author:} TaranenkoAnt
 * {@code @createDate:} 06.05.2026
 */

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return service.processTransfer(request);
    }
}
