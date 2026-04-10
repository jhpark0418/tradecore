package com.jhpark.tradecore.api.execution;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingCommandFacade;
import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.order.ApplyExecutionResult;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExecutionCommandControllerTest {

    private MockMvc mockMvc;
    private TradingCommandFacade tradingCommandFacade;

    @BeforeEach
    void setUp() {
        tradingCommandFacade = Mockito.mock(TradingCommandFacade.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExecutionCommandController(tradingCommandFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void applyExecution_returns200() throws Exception {
        Account account = new Account(
                new AccountId("demo-user-1"),
                Map.of(
                        Asset.USDT, new Balance(Asset.USDT, new BigDecimal("79000"), BigDecimal.ZERO),
                        Asset.BTC, new Balance(Asset.BTC, new BigDecimal("2.0"), BigDecimal.ZERO),
                        Asset.ETH, new Balance(Asset.ETH, new BigDecimal("10"), BigDecimal.ZERO)
                ),
                2L
        );

        Order order = Order.restore(
                new OrderId("order-001"),
                new AccountId("demo-user-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                OrderStatus.PARTIALLY_FILLED,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                new BigDecimal("0.25"),
                2L
        );

        Execution execution = Execution.create(
                new ExecutionId("exec-001"),
                new OrderId("order-001"),
                new AccountId("demo-user-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("42000"),
                new BigDecimal("0.25"),
                new BigDecimal("10500"),
                Instant.parse("2026-04-10T03:40:00Z")
        );

        given(tradingCommandFacade.applyExecution(
                eq("exec-001"),
                eq("order-001"),
                eq(new BigDecimal("42000")),
                eq(new BigDecimal("0.25"))
        )).willReturn(new ApplyExecutionResult(account, order, execution));

        String requestBody = """
                {
                  "executionId": "exec-001",
                  "orderId": "order-001",
                  "executionPrice": 42000,
                  "executionQty": 0.25
                }
                """;

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-001"))
                .andExpect(jsonPath("$.orderId").value("order-001"))
                .andExpect(jsonPath("$.accountId").value("demo-user-1"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.executionPrice").value(42000))
                .andExpect(jsonPath("$.executionQty").value(0.25))
                .andExpect(jsonPath("$.quoteAmount").value(10500))
                .andExpect(jsonPath("$.orderStatus").value("PARTIALLY_FILLED"))
                .andExpect(jsonPath("$.orderFilledQty").value(0.25))
                .andExpect(jsonPath("$.orderRemainingQty").value(0.25))
                .andExpect(jsonPath("$.balances.USDT.available").value(79000))
                .andExpect(jsonPath("$.balances.BTC.available").value(2.0));
    }

    @Test
    void applyExecution_returns400_whenValidationFails() throws Exception {
        String requestBody = """
                {
                  "executionId": "",
                  "orderId": "",
                  "executionPrice": 0,
                  "executionQty": -1
                }
                """;

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.executionId").value("executionId is required"))
                .andExpect(jsonPath("$.errors.orderId").value("orderId is required"))
                .andExpect(jsonPath("$.errors.executionPrice").value("executionPrice must be greater than 0"))
                .andExpect(jsonPath("$.errors.executionQty").value("executionQty must be greater than 0"));
    }

    @Test
    void applyExecution_returns409_whenConflict() throws Exception {
        given(tradingCommandFacade.applyExecution(
                eq("exec-001"),
                eq("order-001"),
                eq(new BigDecimal("42000")),
                eq(new BigDecimal("0.25"))
        )).willThrow(new ConcurrencyConflictException(
                "duplicate execution with different payload. executionId=exec-001"
        ));

        String requestBody = """
                {
                  "executionId": "exec-001",
                  "orderId": "order-001",
                  "executionPrice": 42000,
                  "executionQty": 0.25
                }
                """;

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrency Conflict"))
                .andExpect(jsonPath("$.detail").value("duplicate execution with different payload. executionId=exec-001"));
    }
}