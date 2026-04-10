package com.jhpark.tradecore.api.order;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingQueryService;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.order.Order;
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
}