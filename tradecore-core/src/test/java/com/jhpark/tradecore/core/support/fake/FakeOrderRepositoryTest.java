package com.jhpark.tradecore.core.support.fake;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FakeOrderRepositoryTest {

    @Test
    void saveNewOrderShouldIncreaseVersionToOne() {
        FakeOrderRepository repository = new FakeOrderRepository();

        OrderId orderId = new OrderId("order-1");
        AccountId accountId = new AccountId("account-1");

        Order newOrder = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        Order saved = repository.save(newOrder);

        assertEquals(1L, saved.getVersion());
        assertEquals(1, repository.size());
    }

    @Test
    void sameOrderVersionCannotBeSavedTwice() {
        FakeOrderRepository repository = new FakeOrderRepository();

        OrderId orderId = new OrderId("order-1");
        AccountId accountId = new AccountId("account-1");

        Order newOrder = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        Order persisted = repository.save(newOrder);

        Order snapshot1 = persisted;
        Order snapshot2 = persisted;

        Order updated1 = snapshot1.applyFill(new BigDecimal("1"));
        Order saved1 = repository.save(updated1);

        assertEquals(2L, saved1.getVersion());
        assertEquals(OrderStatus.PARTIALLY_FILLED, saved1.getStatus());
        assertEquals(0, saved1.getFilledQty().compareTo(new BigDecimal("1")));

        Order updated2 = snapshot2.cancel();

        assertThrows(ConcurrencyConflictException.class, () -> repository.save(updated2));
    }

    @Test
    void saveExistingOrderWithLatestVersionShouldIncreaseVersion() {
        FakeOrderRepository repository = new FakeOrderRepository();

        OrderId orderId = new OrderId("order-1");
        AccountId accountId = new AccountId("account-1");

        Order newOrder = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        Order persisted = repository.save(newOrder);
        Order updated = persisted.applyFill(new BigDecimal("2"));

        Order saved = repository.save(updated);

        assertEquals(2L, saved.getVersion());
        assertEquals(OrderStatus.FILLED, saved.getStatus());
        assertEquals(0, saved.getFilledQty().compareTo(new BigDecimal("2")));
    }
}
