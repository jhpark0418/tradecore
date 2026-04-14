package com.jhpark.tradecore.api.admin;

import com.jhpark.tradecore.api.common.PageResponse;
import com.jhpark.tradecore.api.order.response.OrderSummaryResponse;
import com.jhpark.tradecore.application.TradingQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class AdminOrderQueryController {

    private final TradingQueryService tradingQueryService;

    public AdminOrderQueryController(TradingQueryService tradingQueryService) {
        this.tradingQueryService = tradingQueryService;
    }

    @GetMapping("/api/admin/orders")
    public PageResponse<OrderSummaryResponse> searchOrders(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String side,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page);
        validateSize(size);
        validateCreatedRange(createdFrom, createdTo);

        return PageResponse.from(
                tradingQueryService.searchOrders(
                        symbol,
                        status,
                        side,
                        createdFrom,
                        createdTo,
                        page,
                        size
                ),
                OrderSummaryResponse::from
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

    private void validateCreatedRange(OffsetDateTime createdFrom, OffsetDateTime createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom은 createdTo 이전 이어야 합니다.");
        }
    }
}
