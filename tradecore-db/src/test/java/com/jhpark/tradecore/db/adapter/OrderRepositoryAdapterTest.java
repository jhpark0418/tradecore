package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderRepositoryAdapter.class)
@ContextConfiguration(classes = DbTestApplication.class)
class OrderRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private OrderRepositoryAdapter orderRepositoryAdapter;

    @Test
    void saveAndFindById() {
        Order order = Order.newLimitOrder(
                new OrderId("order-1"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.5")
        );

        Order saved = orderRepositoryAdapter.save(order);
        Optional<Order> found = orderRepositoryAdapter.findById(new OrderId("order-1"));

        assertEquals(0L, saved.getVersion());
        assertTrue(found.isPresent());
        assertEquals("order-1", found.get().getOrderId().value());
        assertEquals("account-1", found.get().getAccountId().value());
        assertEquals(OrderSide.BUY, found.get().getSide());
        assertEquals(OrderStatus.NEW, found.get().getStatus());
        assertEquals(0, found.get().getPrice().compareTo(new BigDecimal("70000")));
        assertEquals(0, found.get().getQty().compareTo(new BigDecimal("0.5")));
        assertEquals(0, found.get().getFilledQty().compareTo(BigDecimal.ZERO));
        assertEquals(0L, found.get().getVersion());
    }

    @Test
    void staleVersionThrowsConcurrencyConflict() {
        Order initial = orderRepositoryAdapter.save(
                Order.newLimitOrder(
                        new OrderId("order-2"),
                        new AccountId("account-1"),
                        new Symbol(Asset.BTC, Asset.USDT),
                        OrderSide.SELL,
                        new BigDecimal("72000"),
                        new BigDecimal("1.0")
                )
        );

        Order updated = initial.applyFill(new BigDecimal("0.2"));
        Order persistedUpdated = orderRepositoryAdapter.save(updated);

        assertEquals(1L, persistedUpdated.getVersion());
        assertEquals(OrderStatus.PARTIALLY_FILLED, persistedUpdated.getStatus());

        Order stale = initial.cancel();

        ConcurrencyConflictException exception = assertThrows(
                ConcurrencyConflictException.class,
                () -> orderRepositoryAdapter.save(stale)
        );

        assertTrue(exception.getMessage().contains("order-2"));
    }
}