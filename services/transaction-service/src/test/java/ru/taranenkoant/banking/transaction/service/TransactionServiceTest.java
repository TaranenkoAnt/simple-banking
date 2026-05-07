package ru.taranenkoant.banking.transaction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taranenkoant.banking.transaction.client.AccountClient;
import ru.taranenkoant.banking.transaction.domain.OutboxEvent;
import ru.taranenkoant.banking.transaction.domain.Transaction;
import ru.taranenkoant.banking.transaction.domain.TransactionStatus;
import ru.taranenkoant.banking.transaction.dto.AccountResponse;
import ru.taranenkoant.banking.transaction.dto.TransactionRequest;
import ru.taranenkoant.banking.transaction.dto.TransactionResponse;
import ru.taranenkoant.banking.transaction.mapper.TransactionMapper;
import ru.taranenkoant.banking.transaction.repository.OutboxEventRepository;
import ru.taranenkoant.banking.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AccountClient accountClient;
    @Mock
    private TransactionMapper mapper;
    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldReturnExistingTransactionForIdempotentRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setIdempotencyKey("key1");
        Transaction exesting = Transaction.builder()
                .id(1L).idempotencyKey("key1").status(TransactionStatus.COMPLETED).build();
        TransactionResponse response = TransactionResponse.builder()
                .id(1L).status("COMPLETED").build();

        when(transactionRepository.findByIdempotencyKey("key1")).thenReturn(Optional.of(exesting));
        when(mapper.toResponse(exesting)).thenReturn(response);

        TransactionResponse result = transactionService.processTransfer(request);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(accountClient, never()).getAccount(anyString());
    }

    @Test
    void shouldCompleteTransferSuccessfully() {
        TransactionRequest request = createRequest("from", "to", BigDecimal.TEN, "key2");
        AccountResponse fromAcc = createAccountResponse("from", BigDecimal.TEN, "ACTIVE");
        AccountResponse toAcc = createAccountResponse("to", BigDecimal.ZERO, "ACTIVE");

        Transaction transaction = createTransaction(TransactionStatus.PENDING, "key2");
        Transaction savedTransaction = createTransaction(TransactionStatus.PENDING, "key2");
        Transaction completedTransaction = createTransaction(TransactionStatus.COMPLETED, "key2");

        TransactionResponse mockResponse = TransactionResponse.builder().status("COMPLETED").build();

        when(transactionRepository.findByIdempotencyKey("key2")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(accountClient.getAccount("from")).thenReturn(fromAcc);
        when(accountClient.getAccount("to")).thenReturn(toAcc);

        when(mapper.toResponse(any(Transaction.class))).thenReturn(mockResponse);

//        lenient().doNothing().when(accountClient).withDraw(eq("from"), any());
//        lenient().doNothing().when(accountClient).withDraw(eq("to"), any());

        TransactionResponse result = transactionService.processTransfer(request);
        assertThat(result).isNotNull();

        verify(accountClient, times(1)).withDraw("from", BigDecimal.TEN);
        verify(accountClient, times(1)).deposit("to", BigDecimal.TEN);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldFailWhenInsufficientFunds() {
        TransactionRequest request = createRequest("from", "to", BigDecimal.valueOf(100), "key3");
        AccountResponse fromAcc = createAccountResponse("from", BigDecimal.ONE, "ACTIVE");
        AccountResponse toAcc = createAccountResponse("to", BigDecimal.ONE, "ACTIVE");

        when(transactionRepository.findByIdempotencyKey("key3")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
        when(accountClient.getAccount("from")).thenReturn(fromAcc);
        when(accountClient.getAccount("to")).thenReturn(toAcc);

        assertThatThrownBy(() -> transactionService.processTransfer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }

    private TransactionRequest createRequest(String from, String to, BigDecimal amount, String idemKey) {
        TransactionRequest r = new TransactionRequest();
        r.setFromAccount(from);
        r.setToAccount(to);
        r.setAmount(amount);
        r.setIdempotencyKey(idemKey);
        return r;
    }

    private AccountResponse createAccountResponse(String accNum, BigDecimal balance, String status) {
        AccountResponse r = new AccountResponse();
        r.setAccountNumber(accNum);
        r.setBalance(balance);
        r.setStatus(status);
        return r;
    }

    private Transaction createTransaction(TransactionStatus status, String idemKey) {
        return Transaction.builder()
                .id(1L)
                .fromAccount("from")
                .toAccount("to")
                .amount(BigDecimal.TEN)
                .fee(BigDecimal.ZERO)
                .status(status)
                .idempotencyKey(idemKey)
                .createdAt(LocalDateTime.now())
                .build();
    }
}