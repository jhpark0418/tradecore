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

class ApplyExecutionServiceTest {

    @Test
    void buyLimitOrderExecutionAtSamePriceDecreasesLockedQuoteAndIncreasesBase() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionResult result = service.apply(new ApplyExecutionCommand(
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-1"),
                orderId,
                new BigDecimal("100"),
                new BigDecimal("1")
        ));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2")));
        assertEquals(0, result.order().getFilledQty().compareTo(new BigDecimal("1")));
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.order().getStatus());
    }

    @Test
    void buyLimitOrderExecutionAtBetterPriceRefundsDifference() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-2");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionResult result = service.apply(new ApplyExecutionCommand(
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-2"),
                orderId,
                new BigDecimal("90"),
                new BigDecimal("1")
        ));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("810")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2")));
        assertEquals(0, result.order().getFilledQty().compareTo(new BigDecimal("1")));
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.order().getStatus());
    }

    @Test
    void sellLimitOrderExecutionAtSamePriceDecreasesLockedBaseAndIncreasesQuote() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-3");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1.5"), new BigDecimal("0.5")))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionResult result = service.apply(new ApplyExecutionCommand(
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-3"),
                orderId,
                new BigDecimal("70000"),
                new BigDecimal("0.2")
        ));

        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("1.5")));
        assertEquals(0, result.account().getBalance(Asset.BTC).getLocked().compareTo(new BigDecimal("0.3")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("15000")));
        assertEquals(0, result.order().getFilledQty().compareTo(new BigDecimal("0.2")));
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.order().getStatus());
    }

    @Test
    void fullExecutionMakesOrderFilled() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-4");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionResult result = service.apply(new ApplyExecutionCommand(
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-4"),
                orderId,
                new BigDecimal("100"),
                new BigDecimal("2")
        ));

        assertEquals(0, result.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, result.account().getBalance(Asset.USDT).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("3")));
        assertEquals(0, result.order().getFilledQty().compareTo(new BigDecimal("2")));
        assertEquals(OrderStatus.FILLED, result.order().getStatus());
    }

    @Test
    void executionQtyCannotExceedRemainingQty() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-5");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        ).applyFill(new BigDecimal("1.5"));

        accountRepository.save(account);
        orderRepository.save(order);

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        new com.jhpark.tradecore.core.execution.ExecutionId("exec-5"),
                        orderId,
                        new BigDecimal("100"),
                        new BigDecimal("1")
                ))
        );
    }

    @Test
    void filledOrderCannotBeExecutedAgain() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-6");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("3"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(IllegalStateException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        new com.jhpark.tradecore.core.execution.ExecutionId("exec-6"),
                        orderId,
                        new BigDecimal("100"),
                        new BigDecimal("0.1")
                ))
        );
    }

    @Test
    void buyLimitOrderCannotBeExecutedAboveLimitPrice() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-7");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        new com.jhpark.tradecore.core.execution.ExecutionId("exec-7"),
                        orderId,
                        new BigDecimal("110"),
                        new BigDecimal("1")
                ))
        );
    }

    @Test
    void sellLimitOrderCannotBeExecutedBelowLimitPrice() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-8");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1.5"), new BigDecimal("0.5")))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

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

        FakeExecutionRepository executionRepository = new FakeExecutionRepository();
        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        new com.jhpark.tradecore.core.execution.ExecutionId("exec-8"),
                        orderId,
                        new BigDecimal("69000"),
                        new BigDecimal("0.2")
                ))
        );
    }

    @Test
    void executionIsRecordedWhenOrderIsExecuted() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-9");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("1")
        );

        accountRepository.save(account);
        orderRepository.save(order);

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionResult result = service.apply(new ApplyExecutionCommand(
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-9"),
                orderId,
                new BigDecimal("95"),
                new BigDecimal("1")
        ));

        assertEquals(orderId, result.execution().getOrderId());
        assertEquals(accountId, result.execution().getAccountId());
        assertEquals(0, result.execution().getExecutionPrice().compareTo(new BigDecimal("95")));
        assertEquals(0, result.execution().getExecutionQty().compareTo(new BigDecimal("1")));
        assertEquals(0, result.execution().getQuoteAmount().compareTo(new BigDecimal("95")));
    }

    @Test
    void duplicateExecutionRequestDoesNotApplySettlementTwice() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-10");
        com.jhpark.tradecore.core.execution.ExecutionId executionId =
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-duplicate-1");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        ApplyExecutionCommand command = new ApplyExecutionCommand(
                executionId,
                orderId,
                new BigDecimal("90"),
                new BigDecimal("1")
        );

        ApplyExecutionResult first = service.apply(command);
        ApplyExecutionResult second = service.apply(command);

        assertEquals(0, first.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("810")));
        assertEquals(0, first.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));
        assertEquals(0, first.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2")));

        assertEquals(0, second.account().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("810")));
        assertEquals(0, second.account().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));
        assertEquals(0, second.account().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2")));

        assertEquals(first.execution().getExecutionId(), second.execution().getExecutionId());
        assertEquals(0, second.order().getFilledQty().compareTo(new BigDecimal("1")));
        assertEquals(OrderStatus.PARTIALLY_FILLED, second.order().getStatus());
    }

    @Test
    void sameExecutionIdCannotBeUsedWithDifferentPayload() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-11");
        com.jhpark.tradecore.core.execution.ExecutionId executionId =
                new com.jhpark.tradecore.core.execution.ExecutionId("exec-duplicate-2");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("800"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

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

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        service.apply(new ApplyExecutionCommand(
                executionId,
                orderId,
                new BigDecimal("90"),
                new BigDecimal("1")
        ));

        assertThrows(IllegalArgumentException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        executionId,
                        orderId,
                        new BigDecimal("91"),
                        new BigDecimal("1")
                ))
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
            storage.put(account.getAccountId().value(), account);
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

    private static class FakeExecutionRepository implements com.jhpark.tradecore.core.application.port.out.ExecutionRepository {
        private final Map<String, com.jhpark.tradecore.core.execution.Execution> storage = new HashMap<>();

        @Override
        public Optional<com.jhpark.tradecore.core.execution.Execution> findById(
                com.jhpark.tradecore.core.execution.ExecutionId executionId
        ) {
            return Optional.ofNullable(storage.get(executionId.value()));
        }

        @Override
        public com.jhpark.tradecore.core.execution.Execution save(
                com.jhpark.tradecore.core.execution.Execution execution
        ) {
            storage.put(execution.getExecutionId().value(), execution);
            return execution;
        }
    }
}