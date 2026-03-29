package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlaceOrderServiceTest {
    @Test
    void buyLimitOrderLocksQuoteAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

        accountRepository.save(account);

        PlaceOrderService service = new PlaceOrderService(accountRepository, orderRepository);

        PlaceOrderCommand command = new PlaceOrderCommand(
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        PlaceOrderResult result = service.place(command);

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
        assertEquals(OrderStatus.NEW, result.order().getStatus());
        assertEquals(OrderType.LIMIT, result.order().getType());
    }

    @Test
    void sellLimitOrderLocksBaseAsset() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("2"), BigDecimal.ZERO));

        accountRepository.save(account);

        PlaceOrderService service = new PlaceOrderService(accountRepository, orderRepository);

        PlaceOrderCommand command = new PlaceOrderCommand(
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.SELL,
                OrderType.LIMIT,
                new BigDecimal("70000"),
                new BigDecimal("0.5")
        );

        PlaceOrderResult result = service.place(command);

        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("1.5")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getLocked().compareTo(new BigDecimal("0.5")));
        assertEquals(OrderStatus.NEW, result.order().getStatus());
    }

    @Test
    void buyLimitOrderFailsWhenQuoteBalanceIsInsufficient() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("100"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

        accountRepository.save(account);

        PlaceOrderService service = new PlaceOrderService(accountRepository, orderRepository);

        PlaceOrderCommand command = new PlaceOrderCommand(
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        assertThrows(IllegalArgumentException.class, () -> service.place(command));
    }

    @Test
    void sellLimitOrderFailsWhenBaseBalanceIsInsufficient() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("0.1"), BigDecimal.ZERO));

        accountRepository.save(account);

        PlaceOrderService service = new PlaceOrderService(accountRepository, orderRepository);

        PlaceOrderCommand command = new PlaceOrderCommand(
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.SELL,
                OrderType.LIMIT,
                new BigDecimal("70000"),
                new BigDecimal("0.5")
        );

        assertThrows(IllegalArgumentException.class, () -> service.place(command));
    }

    @Test
    void marketOrderIsNotSupportedForNow() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

        accountRepository.save(account);

        PlaceOrderService service = new PlaceOrderService(accountRepository, orderRepository);

        PlaceOrderCommand command = new PlaceOrderCommand(
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                OrderType.MARKET,
                null,
                new BigDecimal("0.1")
        );

        assertThrows(UnsupportedOperationException.class, () -> service.place(command));
    }

    private static class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> storage = new HashMap<>();

        @Override
        public Optional<Account> findById(AccountId accountId) {
            return Optional.ofNullable(storage.get(accountId.value()));
        }

        @Override
        public Account save(Account account) {
            storage.put(account.getAccountId().value(), account);
            return account;
        }
    }

    private static class FakeOrderRepository implements OrderRepository {
        private final Map<String, Order> storage = new HashMap<>();

        @Override
        public Order save(Order order) {
            storage.put(order.getOrderId().value(), order);
            return order;
        }
    }
}