package com.jhpark.tradecore.api.account;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingQueryService;
import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.support.ApiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountQueryControllerTest {
    private MockMvc mockMvc;
    private TradingQueryService tradingQueryService;

    @BeforeEach
    void setUp() {
        tradingQueryService = Mockito.mock(TradingQueryService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountQueryController(tradingQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAccount_returns200() throws Exception {
        Account account = ApiTestFixtures.sampleAccount("demo-user-1");

        given(tradingQueryService.getAccount("demo-user-1"))
                .willReturn(account);

        mockMvc.perform(get("/api/accounts/{accountId}", "demo-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("demo-user-1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.balances.USDT.available").value(100000))
                .andExpect(jsonPath("$.balances.USDT.locked").value(0))
                .andExpect(jsonPath("$.balances.USDT.total").value(100000))
                .andExpect(jsonPath("$.balances.BTC.available").value(1.5))
                .andExpect(jsonPath("$.balances.BTC.locked").value(0.1))
                .andExpect(jsonPath("$.balances.BTC.total").value(1.6))
                .andExpect(jsonPath("$.balances.ETH.available").value(10))
                .andExpect(jsonPath("$.balances.ETH.locked").value(0))
                .andExpect(jsonPath("$.balances.ETH.total").value(10));
    }

    @Test
    void getAccount_returns404_whenNotFound() throws Exception {
        given(tradingQueryService.getAccount("missing-account"))
                .willThrow(new ResourceNotFoundException(
                        "계정을 찾을 수 없습니다. accountId=missing-account"
                ));

        mockMvc.perform(get("/api/accounts/{accountId}", "missing-account"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("계정을 찾을 수 없습니다. accountId=missing-account"))
                .andExpect(jsonPath("$.path").value("/api/accounts/missing-account"));
    }
}