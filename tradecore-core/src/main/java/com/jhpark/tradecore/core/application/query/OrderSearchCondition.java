package com.jhpark.tradecore.core.application.query;

public record OrderSearchCondition (
        String accountId,
        String symbol,
        String status,
        String side,
        int page,
        int size
) {
}
