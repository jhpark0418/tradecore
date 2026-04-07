package com.jhpark.tradecore.api.order.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank(message = "accountId is required")
        String accountId,

        @NotBlank(message = "symbol is required")
        String symbol,

        @NotBlank(message = "side is required")
        String side,

        @NotBlank(message = "orderType is required")
        String orderType,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00000001", message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "qty is required")
        @DecimalMin(value = "0.00000001", message = "qty must be greater than 0")
        BigDecimal qty
) {
}
