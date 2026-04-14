package com.jhpark.tradecore.api.order;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingQueryService;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.support.ApiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderQueryControllerTest {

    private MockMvc mockMvc;
    private TradingQueryService tradingQueryService;

    @BeforeEach
    void setUp() {
        tradingQueryService = Mockito.mock(TradingQueryService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderQueryController(tradingQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOrder_returns200() throws Exception {
        Order order = ApiTestFixtures.sampleNewBuyOrder("order-001", "demo-user-1");

        given(tradingQueryService.getOrder("order-001"))
                .willReturn(order);

        mockMvc.perform(get("/api/orders/{orderId}", "order-001"))
                .andExpect(status().isOk())
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
    void getOrder_returns404_whenNotFound() throws Exception {
        given(tradingQueryService.getOrder("missing-order"))
                .willThrow(new ResourceNotFoundException(
                        "주문을 찾을 수 없습니다. orderId=missing-order"
                ));

        mockMvc.perform(get("/api/orders/{orderId}", "missing-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("주문을 찾을 수 없습니다. orderId=missing-order"))
                .andExpect(jsonPath("$.path").value("/api/orders/missing-order"));
    }

    @Test
    void getExecutionsByOrder_returns200() throws Exception {
        List<ExecutionSummary> executions = List.of(
                new ExecutionSummary(
                        "exec-002",
                        "order-001",
                        "demo-user-1",
                        "BTCUSDT",
                        OrderSide.BUY,
                        new BigDecimal("70100"),
                        new BigDecimal("0.2"),
                        new BigDecimal("14020"),
                        Instant.parse("2026-04-13T10:05:00Z")
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
        );

        given(tradingQueryService.getExecutionsByOrder("order-001"))
                .willReturn(executions);

        mockMvc.perform(get("/api/orders/{orderId}/executions", "order-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].executionId").value("exec-002"))
                .andExpect(jsonPath("$[0].orderId").value("order-001"))
                .andExpect(jsonPath("$[0].accountId").value("demo-user-1"))
                .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$[0].side").value("BUY"))
                .andExpect(jsonPath("$[0].executionPrice").value(70100))
                .andExpect(jsonPath("$[0].executionQty").value(0.2))
                .andExpect(jsonPath("$[0].quoteAmount").value(14020))
                .andExpect(jsonPath("$[0].executedAt").value("2026-04-13T10:05:00Z"))
                .andExpect(jsonPath("$[1].executionId").value("exec-001"))
                .andExpect(jsonPath("$[1].executionPrice").value(70000))
                .andExpect(jsonPath("$[1].executionQty").value(0.1))
                .andExpect(jsonPath("$[1].quoteAmount").value(7000))
                .andExpect(jsonPath("$[1].executedAt").value("2026-04-13T10:00:00Z"));
    }

    @Test
    void getExecutionsByOrder_returns404_whenOrderNotFound() throws Exception {
        given(tradingQueryService.getExecutionsByOrder("missing-order"))
                .willThrow(new ResourceNotFoundException(
                        "주문을 찾을 수 없습니다. orderId=missing-order"
                ));

        mockMvc.perform(get("/api/orders/{orderId}/executions", "missing-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("주문을 찾을 수 없습니다. orderId=missing-order"))
                .andExpect(jsonPath("$.path").value("/api/orders/missing-order/executions"));
    }
}