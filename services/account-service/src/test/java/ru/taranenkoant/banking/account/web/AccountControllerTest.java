package ru.taranenkoant.banking.account.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.taranenkoant.banking.account.service.AccountService;
import ru.taranenkoant.banking.account.domain.AccountStatus;
import ru.taranenkoant.banking.account.domain.Currency;
import ru.taranenkoant.banking.account.dto.request.AccountCreateRequest;
import ru.taranenkoant.banking.account.dto.request.AccountStatusUpdateRequest;
import ru.taranenkoant.banking.account.dto.response.AccountResponse;
import ru.taranenkoant.banking.account.exception.AccountNotFoundException;
import ru.taranenkoant.banking.account.AccountServiceApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
//@SpringBootTest(classes = AccountServiceApplication.class)
//@AutoConfigureMockMvc
class AccountControllerTest {

    private final String ACCOUNT_NUMBER = "1234567890";
    private final String OWNER_NAME = "Pashtet";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAccount_ShouldReturnCreated() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setOwnerName(OWNER_NAME);
        request.setInitialBalance(new BigDecimal("100.00"));
        request.setCurrency(Currency.RUB);

        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName(OWNER_NAME)
                .balance(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .status(AccountStatus.ACTIVE)
                .createAt(LocalDateTime.now())
                .version(0)
                .build();

        when(accountService.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void createAccount_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest();
        request.setOwnerName(OWNER_NAME);
        request.setInitialBalance(new BigDecimal("100"));
        request.setCurrency(Currency.RUB);

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccount_ShouldReturnOk() throws  Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName(OWNER_NAME)
                .balance(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .status(AccountStatus.ACTIVE)
                .createAt(LocalDateTime.now())
                .version(0)
                .build();

        when(accountService.getAccountByNumber(ACCOUNT_NUMBER)).thenReturn(response);
        mockMvc.perform(get("/api/v1/accounts/" + ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));
    }

    @Test
    void getAccount_ShouldReturnNotFound_WhenAccountMissing() throws Exception {
        when(accountService.getAccountByNumber("009")).thenThrow(new AccountNotFoundException("009"));
        mockMvc.perform(get("/api/v1/accounts/009"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: 009"));
    }

    @Test
    void updateStatus_ShouldReturnOk() throws Exception {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setStatus(AccountStatus.BLOCKED);

        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName(OWNER_NAME)
                .balance(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .status(AccountStatus.BLOCKED)
                .createAt(LocalDateTime.now())
                .version(0)
                .build();

        when(accountService.updateStatus(ACCOUNT_NUMBER, AccountStatus.BLOCKED)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/accounts/" + ACCOUNT_NUMBER + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }
}