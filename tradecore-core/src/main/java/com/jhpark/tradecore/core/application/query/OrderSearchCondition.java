package com.jhpark.tradecore.core.application.query;

import java.time.OffsetDateTime;

public record OrderSearchCondition (
        String accountId,
        String symbol,
        String status,
        String side,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        int page,
        int size
) {
    public OrderSearchCondition(
            String accountId,
            String symbol,
            String status,
            String side,
            int page,
            int size
    ) {
        this(
                accountId,
                symbol,
                status,
                side,
                null,
                null,
                page,
                size
        );
    }
}
