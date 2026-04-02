package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.support.fake.FakeAccountRepository;
import com.jhpark.tradecore.core.support.fake.FakeOrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                new ExecutionId("exec-1"),
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
                new ExecutionId("exec-2"),
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
                new ExecutionId("exec-3"),
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
                new ExecutionId("exec-4"),
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
                        new ExecutionId("exec-5"),
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
                        new ExecutionId("exec-6"),
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
                        new ExecutionId("exec-7"),
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
                        new ExecutionId("exec-8"),
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
                new ExecutionId("exec-9"),
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
        ExecutionId executionId =
                new ExecutionId("exec-duplicate-1");

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
    void accountSaveConflictShouldBePropagatedWhenApplyingExecution() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-conflict-1");

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

        accountRepository.simulateConcurrentUpdateOnNextSave();

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(ConcurrencyConflictException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        new ExecutionId("exec-conflict-1"),
                        orderId,
                        new BigDecimal("90"),
                        new BigDecimal("1")
                ))
        );

        Account storedAccount = accountRepository.findById(accountId).orElseThrow();
        Order storedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(0, storedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, storedAccount.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
        assertEquals(0, storedAccount.getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("1")));

        assertEquals(OrderStatus.NEW, storedOrder.getStatus());
        assertEquals(0, storedOrder.getFilledQty().compareTo(BigDecimal.ZERO));
    }

    @Test
    void cancelledOrderCannotBeExecuted() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-cancelled-1");
        ExecutionId executionId =
                new ExecutionId("exec-cancelled-1");

        Account account = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1"), BigDecimal.ZERO));

        Order order = Order.newLimitOrder(
                orderId,
                accountId,
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        ).cancel();

        accountRepository.save(account);
        orderRepository.save(order);

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(IllegalStateException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        executionId,
                        orderId,
                        new BigDecimal("100"),
                        new BigDecimal("0.5")
                ))
        );

        Account storedAccount = accountRepository.findById(accountId).orElseThrow();
        Order storedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(0, storedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("1000")));
        assertEquals(0, storedAccount.getBalance(Asset.USDT).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(0, storedAccount.getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("1")));

        assertEquals(OrderStatus.CANCELLED, storedOrder.getStatus());
        assertEquals(0, storedOrder.getFilledQty().compareTo(BigDecimal.ZERO));

        assertEquals(Optional.empty(), executionRepository.findById(executionId));
    }

    @Test
    void sameExecutionIdWithDifferentPayloadShouldFail() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");
        ExecutionId executionId = new ExecutionId("exec-1");

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

        assertThrows(ConcurrencyConflictException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        executionId,
                        orderId,
                        new BigDecimal("95"),
                        new BigDecimal("1")
                ))
        );
    }

    @Test
    void sameExecutionIdWithSamePayloadShouldBeIdempotent() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");
        ExecutionId executionId = new ExecutionId("exec-1");

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

        service.apply(new ApplyExecutionCommand(
                executionId,
                orderId,
                new BigDecimal("90"),
                new BigDecimal("1")
        ));

        Order storedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(0, storedOrder.getFilledQty().compareTo(new BigDecimal("1")));
    }

    @Test
    void applyExecutionShouldUpdateOrderAndPersistExecution() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");
        ExecutionId executionId = new ExecutionId("exec-1");

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

        Order savedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(2L, savedOrder.getVersion());
        assertEquals(OrderStatus.PARTIALLY_FILLED, savedOrder.getStatus());
        assertEquals(0, savedOrder.getFilledQty().compareTo(new BigDecimal("1")));
        assertTrue(executionRepository.findById(executionId).isPresent());
    }

    @Test
    void applyExecutionShouldPropagateAccountVersionConflict() {
        FakeAccountRepository accountRepository = new FakeAccountRepository();
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeExecutionRepository executionRepository = new FakeExecutionRepository();

        AccountId accountId = new AccountId("account-1");
        OrderId orderId = new OrderId("order-1");
        ExecutionId executionId = new ExecutionId("exec-1");

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
        accountRepository.simulateConcurrentUpdateOnNextSave();

        ApplyExecutionService service = new ApplyExecutionService(
                accountRepository,
                orderRepository,
                executionRepository
        );

        assertThrows(ConcurrencyConflictException.class, () ->
                service.apply(new ApplyExecutionCommand(
                        executionId,
                        orderId,
                        new BigDecimal("90"),
                        new BigDecimal("1")
                ))
        );

        Order storedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(OrderStatus.NEW, storedOrder.getStatus());
        assertEquals(0, storedOrder.getFilledQty().compareTo(BigDecimal.ZERO));
        assertTrue(executionRepository.findById(executionId).isEmpty());
    }

    private static class FakeExecutionRepository implements com.jhpark.tradecore.core.application.port.out.ExecutionRepository {
        private final Map<String, Execution> storage = new HashMap<>();

        @Override
        public Optional<Execution> findById(
                ExecutionId executionId
        ) {
            return Optional.ofNullable(storage.get(executionId.value()));
        }

        @Override
        public Execution save(
                Execution execution
        ) {
            Execution existing = storage.get(execution.getExecutionId().value());

            if (existing != null) {
                boolean samePayload =
                        existing.getOrderId().equals(execution.getOrderId())
                                && existing.getExecutionPrice().compareTo(execution.getExecutionPrice()) == 0
                                && existing.getExecutionQty().compareTo(execution.getExecutionQty()) == 0;

                if (!samePayload) {
                    throw new ConcurrencyConflictException(
                            "Execution conflict occurred for executionId=" + execution.getExecutionId().value()
                    );
                }

                return existing;
            }


            storage.put(execution.getExecutionId().value(), execution);
            return execution;
        }
    }
}