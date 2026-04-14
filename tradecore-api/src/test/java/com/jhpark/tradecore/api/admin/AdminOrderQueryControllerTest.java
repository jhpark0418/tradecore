package com.jhpark.tradecore.api.admin;

import com.jhpark.tradecore.api.common.GlobalExceptionHandler;
import com.jhpark.tradecore.application.TradingQueryService;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOrderQueryControllerTest {

    private MockMvc mockMvc;
    private TradingQueryService tradingQueryService;

    @BeforeEach
    void setUp() {
        tradingQueryService = Mockito.mock(TradingQueryService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminOrderQueryController(tradingQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchOrders_returns200() throws Exception {
        OffsetDateTime createdFrom = OffsetDateTime.parse("2026-04-01T00:00:00+09:00");
        OffsetDateTime createdTo = OffsetDateTime.parse("2026-04-14T23:59:59+09:00");
        OffsetDateTime now = OffsetDateTime.parse("2026-04-13T10:00:00Z");

        PageResult<OrderSummary> result = new PageResult<>(
                List.of(
                        new OrderSummary(
                                "order-002",
                                "demo-user-2",
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
                                "BTCUSDT",
                                "BUY",
                                "LIMIT",
                                "PARTIALLY_FILLED",
                                new BigDecimal("42000"),
                                new BigDecimal("0.5"),
                                new BigDecimal("0.2"),
                                new BigDecimal("0.3"),
                                2L,
                                now.minusMinutes(10),
                                now.minusMinutes(1)
                        )
                ),
                0,
                20,
                2L,
                1,
                false
        );

        given(tradingQueryService.searchOrders(
                eq("BTCUSDT"),
                eq("NEW"),
                eq("BUY"),
                eq(createdFrom),
                eq(createdTo),
                eq(0),
                eq(20)
        )).willReturn(result);

        mockMvc.perform(get("/api/admin/orders")
                        .param("symbol", "BTCUSDT")
                        .param("status", "NEW")
                        .param("side", "BUY")
                        .param("createdFrom", "2026-04-01T00:00:00+09:00")
                        .param("createdTo", "2026-04-14T23:59:59+09:00")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("order-002"))
                .andExpect(jsonPath("$.content[0].accountId").value("demo-user-2"))
                .andExpect(jsonPath("$.content[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.content[0].side").value("BUY"))
                .andExpect(jsonPath("$.content[0].orderType").value("LIMIT"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"))
                .andExpect(jsonPath("$.content[0].price").value(43000))
                .andExpect(jsonPath("$.content[0].qty").value(0.3))
                .andExpect(jsonPath("$.content[0].filledQty").value(0))
                .andExpect(jsonPath("$.content[0].remainingQty").value(0.3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        then(tradingQueryService).should().searchOrders(
                "BTCUSDT",
                "NEW",
                "BUY",
                createdFrom,
                createdTo,
                0,
                20
        );
    }

    @Test
    void searchOrders_returns400_whenCreatedFromIsAfterCreatedTo() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("createdFrom", "2026-04-15T00:00:00+09:00")
                        .param("createdTo", "2026-04-14T23:59:59+09:00")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("createdFrom은 createdTo 이전 이어야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/admin/orders"));

        then(tradingQueryService).should(never()).searchOrders(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void searchOrders_returns400_whenPageInvalid() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("page 는 0 이상이어야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/admin/orders"));
    }
}