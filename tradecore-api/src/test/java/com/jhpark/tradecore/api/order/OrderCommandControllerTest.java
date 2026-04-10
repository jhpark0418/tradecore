package com.jhpark.tradecore.api.order;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingCommandFacade;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.order.CancelOrderResult;
import com.jhpark.tradecore.core.application.order.PlaceOrderResult;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;
import com.jhpark.tradecore.support.ApiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCommandControllerTest {

    private MockMvc mockMvc;
    private TradingCommandFacade tradingCommandFacade;

    @BeforeEach
    void setUp() {
        tradingCommandFacade = Mockito.mock(TradingCommandFacade.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderCommandController(tradingCommandFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void placeOrder_returns201() throws Exception {
        Order order = Order.restore(
                new OrderId("order-001"),
                new AccountId("demo-user-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                1L
        );

        given(tradingCommandFacade.placeOrder(
                eq("demo-user-1"),
                eq("BTCUSDT"),
                eq("BUY"),
                eq("LIMIT"),
                eq(new BigDecimal("42000")),
                eq(new BigDecimal("0.5"))
        )).willReturn(new PlaceOrderResult(ApiTestFixtures.sampleAccount("demo-user-1"), order));

        String requestBody = """
                {
                  "accountId": "demo-user-1",
                  "symbol": "BTCUSDT",
                  "side": "BUY",
                  "orderType": "LIMIT",
                  "price": 42000,
                  "qty": 0.5
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-001"))
                .andExpect(jsonPath("$.accountId").value("demo-user-1"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.orderType").value("LIMIT"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.price").value(42000))
                .andExpect(jsonPath("$.qty").value(0.5))
                .andExpect(jsonPath("$.filledQty").value(0))
                .andExpect(jsonPath("$.remainingQty").value(0.5))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void placeOrder_returns400_whenValidationFails() throws Exception {
        String requestBody = """
                {
                  "accountId": "",
                  "symbol": "",
                  "side": "BUY",
                  "orderType": "LIMIT",
                  "price": 0,
                  "qty": -1
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.accountId").value("accountId is required"))
                .andExpect(jsonPath("$.errors.symbol").value("symbol is required"))
                .andExpect(jsonPath("$.errors.price").value("price must be greater than 0"))
                .andExpect(jsonPath("$.errors.qty").value("qty must be greater than 0"));
    }

    @Test
    void placeOrder_returns404_whenAccountNotFound() throws Exception {
        given(tradingCommandFacade.placeOrder(
                eq("missing-account"),
                eq("BTCUSDT"),
                eq("BUY"),
                eq("LIMIT"),
                eq(new BigDecimal("42000")),
                eq(new BigDecimal("0.5"))
        )).willThrow(new ResourceNotFoundException(
                "계정을 찾을 수 없습니다. accountId=missing-account"
        ));

        String requestBody = """
                {
                  "accountId": "missing-account",
                  "symbol": "BTCUSDT",
                  "side": "BUY",
                  "orderType": "LIMIT",
                  "price": 42000,
                  "qty": 0.5
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("계정을 찾을 수 없습니다. accountId=missing-account"));
    }

    @Test
    void cancelOrder_returns200() throws Exception {
        Order cancelledOrder = Order.restore(
                new OrderId("order-001"),
                new AccountId("demo-user-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                OrderStatus.CANCELLED,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                new BigDecimal("0.2"),
                2L
        );

        given(tradingCommandFacade.cancelOrder("demo-user-1", "order-001"))
                .willReturn(new CancelOrderResult(
                        ApiTestFixtures.sampleAccount("demo-user-1"),
                        cancelledOrder
                ));

        String requestBody = """
                {
                  "accountId": "demo-user-1"
                }
                """;

        mockMvc.perform(post("/api/orders/{orderId}/cancel", "order-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.filledQty").value(0.2))
                .andExpect(jsonPath("$.remainingQty").value(0.3))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void cancelOrder_returns409_whenConflict() throws Exception {
        given(tradingCommandFacade.cancelOrder("demo-user-1", "order-001"))
                .willThrow(new ConcurrencyConflictException("stale order version. orderId=order-001"));

        String requestBody = """
                {
                  "accountId": "demo-user-1"
                }
                """;

        mockMvc.perform(post("/api/orders/{orderId}/cancel", "order-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrency Conflict"))
                .andExpect(jsonPath("$.detail").value("stale order version. orderId=order-001"));
    }
}