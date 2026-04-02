package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.ExecutionRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class ApplyExecutionService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final Clock clock;

    public ApplyExecutionService(AccountRepository accountRepository, OrderRepository orderRepository, ExecutionRepository executionRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
        this.executionRepository = Objects.requireNonNull(executionRepository, "executionRepository is null");
        clock = Clock.systemUTC();
    }

    public ApplyExecutionService(AccountRepository accountRepository, OrderRepository orderRepository, ExecutionRepository executionRepository, Clock clock) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
        this.executionRepository = Objects.requireNonNull(executionRepository, "executionRepository is null");
        this.clock = Objects.requireNonNull(clock, "clock is null");
    }

    public ApplyExecutionResult apply(ApplyExecutionCommand command) {
        Objects.requireNonNull(command, "command is null");
        Objects.requireNonNull(command.executionId(), "executionId is null");
        Objects.requireNonNull(command.orderId(), "orderId is null");
        requirePositive(command.executionPrice(), "executionPrice");
        requirePositive(command.executionQty(), "executionQty");

        Execution existingExecution = executionRepository.findById(command.executionId()).orElse(null);
        if (existingExecution != null) {
            return handleDuplicateExecution(existingExecution, command);
        }

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + command.orderId().value()
                ));

        if (order.getType() != OrderType.LIMIT) {
            throw new UnsupportedOperationException("현재 단계에서는 LIMIT 주문 체결만 지원합니다.");
        }

        if (order.isClosed()) {
            throw new IllegalStateException("종결된 주문에는 체결을 적용할 수 없습니다. status=" + order.getStatus());
        }

        if (order.remainingQty().compareTo(command.executionQty()) < 0) {
            throw new IllegalArgumentException("체결 수량은 미체결 수량보다 클 수 없습니다.");
        }

        validateExecutionPrice(order, command.executionPrice());

        Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "계정을 찾을 수 없습니다. accountId=" + order.getAccountId().value()
                ));

        Account settledAccount = settle(account, order, command.executionPrice(), command.executionQty());
        Order executedOrder = order.applyFill(command.executionQty());

        Execution execution = Execution.create(
                command.executionId(),
                order.getOrderId(),
                order.getAccountId(),
                order.getSymbol(),
                order.getSide(),
                command.executionPrice(),
                command.executionQty(),
                calculateExecutedQuoteAmount(command.executionPrice(), command.executionQty()),
                Instant.now(clock)
        );

        Account savedAccount = accountRepository.save(settledAccount);
        Order savedOrder = orderRepository.save(executedOrder);
        Execution savedExecution = executionRepository.save(execution);

        return new ApplyExecutionResult(savedAccount, savedOrder, savedExecution);
    }

    private ApplyExecutionResult handleDuplicateExecution(Execution existingExecution, ApplyExecutionCommand command) {
        validateDuplicateRequest(existingExecution, command);

        Order existingOrder = orderRepository.findById(existingExecution.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "기존 체결에 연결된 주문을 찾을 수 없습니다. orderId=" + existingExecution.getOrderId().value()
                ));

        Account existingAccount = accountRepository.findById(existingExecution.getAccountId())
                .orElseThrow(() -> new IllegalStateException(
                        "기존 체결에 연결된 계정을 찾을 수 없습니다. accountId=" + existingExecution.getAccountId().value()
                ));

        return new ApplyExecutionResult(existingAccount, existingOrder, existingExecution);
    }

    private void validateDuplicateRequest(Execution existingExecution, ApplyExecutionCommand command) {
        if (!existingExecution.getOrderId().equals(command.orderId())) {
            throw new ConcurrencyConflictException(
                    "같은 executionId로 다른 orderId를 사용할 수 없습니다. executionId=" + command.executionId().value()
            );
        }

        if (existingExecution.getExecutionPrice().compareTo(command.executionPrice()) != 0) {
            throw new ConcurrencyConflictException(
                    "같은 executionId로 다른 executionPrice를 사용할 수 없습니다. executionId=" + command.executionId().value()
            );
        }

        if (existingExecution.getExecutionQty().compareTo(command.executionQty()) != 0) {
            throw new ConcurrencyConflictException(
                    "같은 executionId로 다른 executionQty를 사용할 수 없습니다. executionId=" + command.executionId().value()
            );
        }
    }

    private void validateExecutionPrice(Order order, BigDecimal executionPrice) {
        if (order.isBuy() && executionPrice.compareTo(order.getPrice()) > 0) {
            throw new IllegalArgumentException("BUY LIMIT 주문은 주문 가격보다 높은 가격으로 체결될 수 없습니다.");
        }

        if (order.isSell() && executionPrice.compareTo(order.getPrice()) < 0) {
            throw new IllegalArgumentException("SELL LIMIT 주문은 주문 가격보다 낮은 가격으로 체결될 수 없습니다.");
        }
    }

    private Account settle(Account account, Order order, BigDecimal executionPrice, BigDecimal executionQty) {
        if (order.isBuy()) {
            return settleBuy(account, order, executionPrice, executionQty);
        }
        return settleSell(account, order, executionPrice, executionQty);
    }

    private Account settleBuy(Account account, Order order, BigDecimal executionPrice, BigDecimal executionQty) {
        Asset quoteAsset = order.getSymbol().quoteAsset();
        Asset baseAsset = order.getSymbol().baseAsset();

        BigDecimal reservedQuoteAmount = calculateReservedQuoteAmount(order, executionQty);
        BigDecimal executedQuoteAmount = calculateExecutedQuoteAmount(executionPrice, executionQty);
        BigDecimal refundAmount = reservedQuoteAmount.subtract(executedQuoteAmount);

        Account settled = account.decreaseLocked(quoteAsset, reservedQuoteAmount)
                .increaseAvailable(baseAsset, executionQty);

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            settled = settled.increaseAvailable(quoteAsset, refundAmount);
        }

        return settled;
    }

    private Account settleSell(Account account, Order order, BigDecimal executionPrice, BigDecimal executionQty) {
        Asset quoteAsset = order.getSymbol().quoteAsset();
        Asset baseAsset = order.getSymbol().baseAsset();

        BigDecimal executedQuoteAmount = calculateExecutedQuoteAmount(executionPrice, executionQty);

        return account.decreaseLocked(baseAsset, executionQty)
                .increaseAvailable(quoteAsset, executedQuoteAmount);
    }

    private BigDecimal calculateReservedQuoteAmount(Order order, BigDecimal executionQty) {
        return order.getPrice().multiply(executionQty).stripTrailingZeros();
    }

    private BigDecimal calculateExecutedQuoteAmount(BigDecimal executionPrice, BigDecimal executionQty) {
        return executionPrice.multiply(executionQty).stripTrailingZeros();
    }

    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is null");
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " 은(는) 0보다 커야 합니다.");
        }
    }
}
