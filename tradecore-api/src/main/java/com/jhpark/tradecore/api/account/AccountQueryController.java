package com.jhpark.tradecore.api.account;

import com.jhpark.tradecore.api.account.response.AccountResponse;
import com.jhpark.tradecore.api.common.PageResponse;
import com.jhpark.tradecore.api.execution.response.ExecutionSummaryResponse;
import com.jhpark.tradecore.api.order.response.OrderSummaryResponse;
import com.jhpark.tradecore.application.TradingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountQueryController {

    private final TradingQueryService tradingQueryService;

    public AccountQueryController(TradingQueryService tradingQueryService) {
        this.tradingQueryService = tradingQueryService;
    }

    @GetMapping("/api/accounts/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return AccountResponse.from(tradingQueryService.getAccount(accountId));
    }

    @GetMapping("/api/accounts/{accountId}/orders")
    public PageResponse<OrderSummaryResponse> getOrders(
            @PathVariable String accountId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String side,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page);
        validateSize(size);

        return PageResponse.from(
                tradingQueryService.getOrders(accountId, symbol, status, side, page, size),
                OrderSummaryResponse::from
        );
    }

    @GetMapping("/api/accounts/{accountId}/executions")
    public PageResponse<ExecutionSummaryResponse> getExecutionsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page);
        validateSize(size);

        return PageResponse.from(
                tradingQueryService.getExecutionsByAccount(accountId, page, size),
                ExecutionSummaryResponse::from
        );
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
    }

    private void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size 는 1 이상 100 이하이어야 합니다.");
        }
    }
}
