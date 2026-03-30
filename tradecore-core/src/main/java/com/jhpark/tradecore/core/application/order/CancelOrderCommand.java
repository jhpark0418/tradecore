package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.order.OrderId;

public record CancelOrderCommand(
        AccountId accountId,
        OrderId orderId
) {

}
