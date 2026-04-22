package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.support.fake.FakeAccountRepository;
import com.jhpark.tradecore.core.support.fake.FakeOrderRepository;
import com.jhpark.tradecore.core.support.fake.FakeOutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderServiceTest {

    @Test
    void cancelBuyOrderUnlocksRemainingQuoteAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        CancelOrderResult result = service.cancel(new CancelOrderCommand(accountId, orderId));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("1000")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(OrderStatus.CANCELLED, result.order().getStatus());
    }

    @Test
    void cancelSellOrderUnlocksRemainingBaseAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        CancelOrderResult result = service.cancel(new CancelOrderCommand(accountId, orderId));

        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2.0")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(OrderStatus.CANCELLED, result.order().getStatus());
    }

    @Test
    void cancelPartiallyFilledBuyOrderUnlocksOnlyRemainingQuoteAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

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
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancel(new CancelOrderCommand(accountId, orderId))
        );
    }

    @Test
    void orderOwnerMustMatch() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancel(new CancelOrderCommand(otherId, orderId))
        );
    }

    @Test
    void accountSaveConflictShouldBePropagatedWhenCancellingOrder() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-conflict-1");

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

        accountRepository.simulateConcurrentUpdateOnNextSave();

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        assertThrows(ConcurrencyConflictException.class, () ->
                service.cancel(new CancelOrderCommand(accountId, orderId))
        );

        Account storedAccount = accountRepository.findById(accountId).orElseThrow();
        Order storedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(0, storedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, storedAccount.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
        assertEquals(OrderStatus.NEW, storedOrder.getStatus());
    }

    @Test
    void cancelOrderShouldUnlockRemainingQuoteBalance() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        service.cancel(new CancelOrderCommand(accountId, orderId));

        Account savedAccount = accountRepository.findById(accountId).orElseThrow();
        Order savedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(2L, savedAccount.getVersion());
        assertEquals(0, savedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("1000")));
        assertEquals(0, savedAccount.getBalance(Asset.USDT).getLocked().compareTo(BigDecimal.ZERO));

        assertEquals(2L, savedOrder.getVersion());
        assertEquals(OrderStatus.CANCELLED, savedOrder.getStatus());
    }

    @Test
    void cancelOrderShouldPropagateAccountVersionConflict() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();

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
        accountRepository.simulateConcurrentUpdateOnNextSave();

        CancelOrderService service = new CancelOrderService(accountRepository, orderRepository, outboxEventRepository);

        assertThrows(ConcurrencyConflictException.class, () ->
                service.cancel(new CancelOrderCommand(accountId, orderId))
        );

        Account storedAccount = accountRepository.findById(accountId).orElseThrow();
        Order storedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(0, storedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, storedAccount.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
        assertEquals(OrderStatus.NEW, storedOrder.getStatus());
    }
}