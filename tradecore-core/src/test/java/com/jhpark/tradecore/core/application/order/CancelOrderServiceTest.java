package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderServiceTest {

    @Test
    void cancelBuyOrderUnlocksRemainingQuoteAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        accountRepository.save(account);
        orderRepository.save(order);

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository);

        CancelOrderResult result = service.cancel(new CancelOrderCommand(accountId, orderId));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("1000")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(OrderStatus.CANCELLED, result.order().getStatus());
    }

    @Test
    void cancelSellOrderUnlocksRemainingBaseAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-2");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1.5"), new BigDecimal("0.5")));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.SELL,
                new BigDecimal("70000"),
                new BigDecimal("0.5")
        );

        accountRepository.save(account);
        orderRepository.save(order);

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository);

        CancelOrderResult result = service.cancel(new CancelOrderCommand(accountId, orderId));

        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2.0")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(OrderStatus.CANCELLED, result.order().getStatus());
    }

    @Test
    void cancelPartiallyFilledBuyOrderUnlocksOnlyRemainingQuoteAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-3");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        ).applyFill(new BigDecimal("0.5"));

        accountRepository.save(account);
        orderRepository.save(order);

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository);

        CancelOrderResult result = service.cancel(new CancelOrderCommand(accountId, orderId));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("950")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("50")));
        assertEquals(OrderStatus.CANCELLED, result.order().getStatus());
        assertEquals(0, result.order().getFilledQty().compareTo(new BigDecimal("0.5")));
    }

    @Test
    void filledOrderCannotBeCancelled() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-4");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        ).applyFill(new BigDecimal("2"));

        accountRepository.save(account);
        orderRepository.save(order);

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancel(new CancelOrderCommand(accountId, orderId))
        );
    }

    @Test
    void orderOwnerMustMatch() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId ownerId = new AccountId("account-1");
        AccountId otherId = new AccountId("account-2");
        OrderId orderId = new OrderId("order-5");

        accountRepository.save(Account.empty(ownerId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200"))));
        accountRepository.save(Account.empty(otherId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO)));

        Order order = Order.newLimitOrder(
                orderId,
                ownerId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        orderRepository.save(order);

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancel(new CancelOrderCommand(otherId, orderId))
        );
    }

    private static class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> storage = new HashMap<>();

        @Override
        public Optional<Account> findById(AccountId accountId) {
            return Optional.ofNullable(storage.get(accountId.value()));
        }

        @Override
        public Account save(Account account) {
            storage.put(account.getAccountId().value(), account);;
            return account;
        }
    }

    private static class FakeOrderRepository implements OrderRepository {
        private final Map<String, Order> storage = new HashMap<>();

        @Override
        public Optional<Order> findById(OrderId orderId) {
            return Optional.ofNullable(storage.get(orderId.value()));
        }

        @Override
        public Order save(Order order) {
            storage.put(order.getOrderId().value(), order);
            return order;
        }
    }
}