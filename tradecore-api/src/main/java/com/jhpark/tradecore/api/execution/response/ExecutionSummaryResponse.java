package com.jhpark.tradecore.api.execution.response;

import com.jhpark.tradecore.core.application.query.ExecutionSummary;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionSummaryResponse(
        String executionId,
        String orderId,
        String accountId,
        String symbol,
        String side,
        BigDecimal executionPrice,
        BigDecimal executionQty,
        BigDecimal quoteAmount,
        Instant executedAt
) {
    public static ExecutionSummaryResponse from(ExecutionSummary summary) {
        return new ExecutionSummaryResponse(
                summary.executionId(),
                summary.orderId(),
                summary.accountId(),
                summary.symbol(),
                summary.side().name(),
                summary.executionPrice(),
                summary.executionQty(),
                summary.quoteAmount(),
                summary.executedAt()
        );
    }
}
