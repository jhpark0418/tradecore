package com.jhpark.tradecore.api.account;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingQueryService;
import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.support.ApiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void getOrders_returns200() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-13T10:00:00Z");

        PageResult<OrderSummary> result = new PageResult<>(
                List.of(
                        new OrderSummary(
                                "order-002",
                                "demo-user-1",
                                "BTCUSDT",
                                "BUY",
                                "LIMIT",
                                "NEW",
                                new BigDecimal("43000"),
                                new BigDecimal("0.3"),
                                BigDecimal.ZERO,
                                new BigDecimal("0.3"),
                                1L,
                                now,
                                now
                        ),
                        new OrderSummary(
                                "order-001",
                                "demo-user-1",
                                "ETHUSDT",
                                "SELL",
                                "LIMIT",
                                "PARTIALLY_FILLED",
                                new BigDecimal("3200"),
                                new BigDecimal("2"),
                                new BigDecimal("0.5"),
                                new BigDecimal("1.5"),
                                2L,
                                now.minusMinutes(5),
                                now.minusMinutes(1)
                        )
                ),
                0,
                20,
                2,
                1,
                false
        );

        given(tradingQueryService.getOrders(
                eq("demo-user-1"),
                eq("BTCUSDT"),
                eq("NEW"),
                eq("BUY"),
                eq(0),
                eq(20)
        )).willReturn(result);

        mockMvc.perform(get("/api/accounts/{accountId}/orders", "demo-user-1")
                        .param("symbol", "BTCUSDT")
                        .param("status", "NEW")
                        .param("side", "BUY")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("order-002"))
                .andExpect(jsonPath("$.content[0].accountId").value("demo-user-1"))
                .andExpect(jsonPath("$.content[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.content[0].side").value("BUY"))
                .andExpect(jsonPath("$.content[0].orderType").value("LIMIT"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"))
                .andExpect(jsonPath("$.content[0].price").value(43000))
                .andExpect(jsonPath("$.content[0].qty").value(0.3))
                .andExpect(jsonPath("$.content[0].filledQty").value(0))
                .andExpect(jsonPath("$.content[0].remainingQty").value(0.3))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getOrders_returns404_whenAccountNotFound() throws Exception {
        given(tradingQueryService.getOrders(
                eq("missing-account"),
                eq(null),
                eq(null),
                eq(null),
                eq(0),
                eq(20)
        )).willThrow(new ResourceNotFoundException(
                "계정을 찾을 수 없습니다. accountId=missing-account"
        ));

        mockMvc.perform(get("/api/accounts/{accountId}/orders", "missing-account"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("계정을 찾을 수 없습니다. accountId=missing-account"))
                .andExpect(jsonPath("$.path").value("/api/accounts/missing-account/orders"));
    }

    @Test
    void getOrders_returns400_whenPageInvalid() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/orders", "demo-user-1")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("page 는 0 이상이어야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/accounts/demo-user-1/orders"));
    }

    @Test
    void getOrders_returns400_whenSizeInvalid() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/orders", "demo-user-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("size 는 1 이상 100 이하이어야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/accounts/demo-user-1/orders"));
    }

    @Test
    void getExecutionsByAccount_returns200() throws Exception {
        PageResult<ExecutionSummary> result = new PageResult<>(
                List.of(
                        new ExecutionSummary(
                                "exec-002",
                                "order-002",
                                "demo-user-1",
                                "ETHUSDT",
                                OrderSide.SELL,
                                new BigDecimal("3500"),
                                new BigDecimal("1.2"),
                                new BigDecimal("4200"),
                                Instant.parse("2026-04-13T11:00:00Z")
                        ),
                        new ExecutionSummary(
                                "exec-001",
                                "order-001",
                                "demo-user-1",
                                "BTCUSDT",
                                OrderSide.BUY,
                                new BigDecimal("70000"),
                                new BigDecimal("0.1"),
                                new BigDecimal("7000"),
                                Instant.parse("2026-04-13T10:00:00Z")
                        )
                ),
                0,
                20,
                2L,
                1,
                false
        );

        given(tradingQueryService.getExecutionsByAccount("demo-user-1", 0, 20))
                .willReturn(result);

        mockMvc.perform(get("/api/accounts/{accountId}/executions", "demo-user-1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].executionId").value("exec-002"))
                .andExpect(jsonPath("$.content[0].orderId").value("order-002"))
                .andExpect(jsonPath("$.content[0].accountId").value("demo-user-1"))
                .andExpect(jsonPath("$.content[0].symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.content[0].side").value("SELL"))
                .andExpect(jsonPath("$.content[0].executionPrice").value(3500))
                .andExpect(jsonPath("$.content[0].executionQty").value(1.2))
                .andExpect(jsonPath("$.content[0].quoteAmount").value(4200))
                .andExpect(jsonPath("$.content[0].executedAt").value("2026-04-13T11:00:00Z"))
                .andExpect(jsonPath("$.content[1].executionId").value("exec-001"))
                .andExpect(jsonPath("$.content[1].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getExecutionsByAccount_returns404_whenAccountNotFound() throws Exception {
        given(tradingQueryService.getExecutionsByAccount("missing-account", 0, 20))
                .willThrow(new ResourceNotFoundException(
                        "계정을 찾을 수 없습니다. accountId=missing-account"
                ));

        mockMvc.perform(get("/api/accounts/{accountId}/executions", "missing-account")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("계정을 찾을 수 없습니다. accountId=missing-account"))
                .andExpect(jsonPath("$.path").value("/api/accounts/missing-account/executions"));
    }

    @Test
    void getExecutionsByAccount_returns400_whenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/executions", "demo-user-1")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));
    }
}