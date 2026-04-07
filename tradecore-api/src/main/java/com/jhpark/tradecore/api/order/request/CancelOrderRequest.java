package com.jhpark.tradecore.api.order.request;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank(message = "accountId is required")
        String accountId
) {
}