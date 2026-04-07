package com.jhpark.tradecore.api.order.response;

import com.jhpark.tradecore.core.order.Order;

import java.math.BigDecimal;

public record OrderResponse(
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
        long version
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId().value(),
                order.getAccountId().value(),
                order.getSymbol().value(),
                order.getSide().name(),
                order.getType().name(),
                order.getStatus().name(),
                order.getPrice(),
                order.getQty(),
                order.getFilledQty(),
                order.remainingQty(),
                order.getVersion()
        );
    }
}