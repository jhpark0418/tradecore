package com.jhpark.tradecore.core.application.query;

import com.jhpark.tradecore.core.order.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionSummary(
        String executionId,
        String orderId,
        String accountId,
        String symbol,
        OrderSide side,
        BigDecimal executionPrice,
        BigDecimal executionQty,
        BigDecimal quoteAmount,
        Instant executedAt
) {
}
