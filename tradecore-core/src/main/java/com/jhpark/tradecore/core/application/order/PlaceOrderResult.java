package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.order.Order;

public record PlaceOrderResult(
        Account account,
        Order order
) {

}
