package com.jhpark.tradecore.core.order;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.market.Symbol;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void limitOrderCanBeCreated() {
        Order order = Order.newLimitOrder(
                new OrderId("order-1"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.01")
        );

        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(0, order.getQty().compareTo(new BigDecimal("0.01")));
        assertEquals(0, order.getFilledQty().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.remainingQty().compareTo(new BigDecimal("0.01")));
    }

    @Test
    void marketOrderCanBeCreated() {
        Order order = Order.newMarketOrder(
                new OrderId("order-2"),
                new AccountId("account-1"),
                new Symbol(Asset.ETH, Asset.USDT),
                OrderSide.SELL,
                new BigDecimal("0.5")
        );

        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertNull(order.getPrice());
        assertEquals(0, order.getQty().compareTo(new BigDecimal("0.5")));
    }

    @Test
    void limitOrderRequiresPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                Order.newLimitOrder(
                        new OrderId("order-3"),
                        new AccountId("account-1"),
                        new Symbol(Asset.BTC, Asset.USDT),
                        OrderSide.BUY,
                        null,
                        new BigDecimal("0.01")
                )
        );
    }

    @Test
    void qtyMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class, () ->
                Order.newLimitOrder(
                        new OrderId("order-5"),
                        new AccountId("account-1"),
                        new Symbol(Asset.BTC, Asset.USDT),
                        OrderSide.BUY,
                        new BigDecimal("70000"),
                        BigDecimal.ZERO
                )
        );
    }
}