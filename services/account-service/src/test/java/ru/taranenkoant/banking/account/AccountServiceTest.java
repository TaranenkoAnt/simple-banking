package ru.taranenkoant.banking.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taranenkoant.banking.account.domain.Account;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;
import ru.taranenkoant.banking.account.exception.InsufficientFundsException;
import ru.taranenkoant.banking.account.mapper.AccountMapper;
import ru.taranenkoant.banking.account.repository.AccountRepository;
import ru.taranenkoant.banking.account.service.AccountService;

import java.math.BigDecimal;
import java.util.Optional;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private final String ACCOUNT_NUMBER = "1234567890";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private Account account;
    private AccountCreateRequest createRequest;
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {

        account = Account.builder()
                .id(1L)
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName("Test user")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        createRequest = new AccountCreateRequest();
        createRequest.setAccountNumber(ACCOUNT_NUMBER);
        createRequest.setOwnerName("Test user");
        createRequest.setInitialBalance(new BigDecimal("100.00"));
        createRequest.setCurrency(Currency.USD);

        accountResponse = AccountResponse.builder().id(1L)
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName("Test user")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        lenient().when(accountMapper.toEntity(createRequest)).thenReturn(account);
        lenient().when(accountMapper.toResponse(account)).thenReturn(accountResponse);
    }

    @Test
    void createAccount_ShouldReturnAccountResponse() {
        when(accountRepository.existsByAccountNumber("1234567890")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse result = accountService.createAccount(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getAccountByNumber_ShouldReturnResponse_WhenExists() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        AccountResponse result = accountService.getAccountByNumber(ACCOUNT_NUMBER);

        assertThat(result.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
    }

    @Test
    void withdraw_ShouldReduceBalance_WhenSufficientFunds() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw(ACCOUNT_NUMBER, new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_ShouldThrowInsufficientFunds_WhenBalanceTooLow() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(ACCOUNT_NUMBER, new BigDecimal("200.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }
}