package com.jhpark.tradecore.api.order.response;

import com.jhpark.tradecore.core.application.query.OrderSummary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryResponse(
        String orderId,
        String accountId,
        String symbol,
        String side,
        String orderType,
        String status,
        BigDecimal price,
        BigDecimal qty,
        BigDecimal filledQty,
        BigDecimal remainingQty,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderSummaryResponse from(OrderSummary summary) {
        return new OrderSummaryResponse(
                summary.orderId(),
                summary.accountId(),
                summary.symbol(),
                summary.side(),
                summary.orderType(),
                summary.status(),
                summary.price(),
                summary.qty(),
                summary.filledQty(),
                summary.remainingQty(),
                summary.version(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }
}
