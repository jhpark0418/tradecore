package com.jhpark.tradecore.api.execution.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApplyExecutionRequest(
        @NotBlank(message = "executionId is required")
        String executionId,

        @NotBlank(message = "orderId is required")
        String orderId,

        @NotNull(message = "executionPrice is required")
        @DecimalMin(value = "0.00000001", message = "executionPrice must be greater than 0")
        BigDecimal executionPrice,

        @NotNull(message = "executionQty is required")
        @DecimalMin(value = "0.00000001", message = "executionQty must be greater than 0")
        BigDecimal executionQty
) {

}
