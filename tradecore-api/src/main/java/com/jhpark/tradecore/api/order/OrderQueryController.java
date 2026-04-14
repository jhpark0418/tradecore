package com.jhpark.tradecore.api.order;

import com.jhpark.tradecore.api.execution.response.ExecutionSummaryResponse;
import com.jhpark.tradecore.api.order.response.OrderResponse;
import com.jhpark.tradecore.application.TradingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderQueryController {

    private final TradingQueryService tradingQueryService;

    public OrderQueryController(TradingQueryService tradingQueryService) {
        this.tradingQueryService = tradingQueryService;
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return OrderResponse.from(tradingQueryService.getOrder(orderId));
    }

    @GetMapping("/api/orders/{orderId}/executions")
    public List<ExecutionSummaryResponse> getExecutionsByOrder(@PathVariable String orderId) {
        return tradingQueryService.getExecutionsByOrder(orderId).stream()
                .map(ExecutionSummaryResponse::from)
                .toList();
    }
}
