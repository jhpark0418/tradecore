package com.jhpark.tradecore.core.application.query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummary(
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
}
