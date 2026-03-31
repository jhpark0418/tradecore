package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.order.Order;

public record ApplyExecutionResult(
        Account account,
        Order order,
        Execution execution
) {
}
