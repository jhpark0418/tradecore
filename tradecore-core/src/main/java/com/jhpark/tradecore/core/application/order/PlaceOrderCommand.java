package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderType;

import java.math.BigDecimal;

public record PlaceOrderCommand(
        AccountId accountId,
        Symbol symbol,
        OrderSide side,
        OrderType orderType,
        BigDecimal price,
        BigDecimal qty
) {
}
