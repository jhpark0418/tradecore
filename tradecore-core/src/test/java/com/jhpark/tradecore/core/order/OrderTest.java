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

    @Test
    void applyPartialFillChangesStatusToPartiallyFilled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-10"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        );

        Order filled = order.applyFill(new BigDecimal("0.3"));

        assertEquals(OrderStatus.PARTIALLY_FILLED, filled.getStatus());
        assertEquals(0, filled.getFilledQty().compareTo(new BigDecimal("0.3")));
        assertEquals(0, filled.remainingQty().compareTo(new BigDecimal("0.7")));
    }

    @Test
    void applyFullFillChangesStatusToFilled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-11"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        );

        Order filled = order.applyFill(new BigDecimal("1"));

        assertEquals(OrderStatus.FILLED, filled.getStatus());
        assertEquals(0, filled.getFilledQty().compareTo(new BigDecimal("1")));
        assertEquals(0, filled.remainingQty().compareTo(BigDecimal.ZERO));
    }

    @Test
    void multipleFillsCanAccumulateToFilled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-12"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.SELL,
                new BigDecimal("70000"),
                new BigDecimal("1")
        );

        Order partiallyFilled = order.applyFill(new BigDecimal("0.4"));
        Order fullyFilled = partiallyFilled.applyFill(new BigDecimal("0.6"));

        assertEquals(OrderStatus.PARTIALLY_FILLED, partiallyFilled.getStatus());
        assertEquals(OrderStatus.FILLED, fullyFilled.getStatus());
        assertEquals(0, fullyFilled.getFilledQty().compareTo(new BigDecimal("1")));
        assertEquals(0, fullyFilled.remainingQty().compareTo(BigDecimal.ZERO));
    }

    @Test
    void cancelNewOrderChangesStatusToCancelled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-13"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        );

        Order cancelled = order.cancel();

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void partiallyFilledOrderCanBeCancelled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-14"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        ).applyFill(new BigDecimal("0.2"));

        Order cancelled = order.cancel();

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, cancelled.getFilledQty().compareTo(new BigDecimal("0.2")));
    }

    @Test
    void filledOrderCannotBeCancelled() {
        Order order = Order.newLimitOrder(
                new OrderId("order-15"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        ).applyFill(new BigDecimal("1"));

        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void cancelledOrderCannotReceiveFill() {
        Order order = Order.newLimitOrder(
                new OrderId("order-16"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        ).cancel();

        assertThrows(IllegalStateException.class, () -> order.applyFill(new BigDecimal("0.1")));
    }

    @Test
    void fillQtyCannotExceedRemainingQty() {
        Order order = Order.newLimitOrder(
                new OrderId("order-17"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        ).applyFill(new BigDecimal("0.7"));

        assertThrows(IllegalArgumentException.class, () -> order.applyFill(new BigDecimal("0.4")));
    }

    @Test
    void fillQtyMustBePositive() {
        Order order = Order.newLimitOrder(
                new OrderId("order-18"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("1")
        );

        assertThrows(IllegalArgumentException.class, () -> order.applyFill(BigDecimal.ZERO));
    }
}