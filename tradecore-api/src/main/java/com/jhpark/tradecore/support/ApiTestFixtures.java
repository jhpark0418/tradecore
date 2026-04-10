package com.jhpark.tradecore.support;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;

import java.math.BigDecimal;
import java.util.Map;

public class ApiTestFixtures {

    private ApiTestFixtures() {}

    public static Account sampleAccount(String accountId) {
        return new Account(
                new AccountId(accountId),
                Map.of(
                        Asset.USDT, new Balance(Asset.USDT, new BigDecimal("100000"), BigDecimal.ZERO),
                        Asset.BTC, new Balance(Asset.BTC, new BigDecimal("1.5"), new BigDecimal("0.1")),
                        Asset.ETH, new Balance(Asset.ETH, new BigDecimal("10"), BigDecimal.ZERO)
                ),
                1L
        );
    }

    public static Order sampleNewBuyOrder(String orderId, String accountId) {
        return Order.restore(
                new OrderId(orderId),
                new AccountId(accountId),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                1L
        );
    }
}
