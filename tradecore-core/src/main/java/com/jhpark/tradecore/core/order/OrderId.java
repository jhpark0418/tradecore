package com.jhpark.tradecore.core.order;

import java.util.UUID;

public record OrderId(String value) {

    public OrderId {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("주문 ID는 비어 있을 수 없습니다.");
        }
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID().toString());
    }
}
