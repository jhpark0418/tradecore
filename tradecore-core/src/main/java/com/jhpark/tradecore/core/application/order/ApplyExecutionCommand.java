package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.order.OrderId;

import java.math.BigDecimal;

public record ApplyExecutionCommand(
        ExecutionId executionId,
        OrderId orderId,
        BigDecimal executionPrice,
        BigDecimal executionQty
) {

}
