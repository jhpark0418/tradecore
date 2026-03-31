package com.jhpark.tradecore.core.execution;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Execution {

    private final ExecutionId executionId;
    private final OrderId orderId;
    private final AccountId accountId;
    private final Symbol symbol;
    private final OrderSide side;
    private final BigDecimal executionPrice;
    private final BigDecimal executionQty;
    private final BigDecimal quoteAmount;
    private final Instant executedAt;

    private Execution(
            ExecutionId executionId,
            OrderId orderId,
            AccountId accountId,
            Symbol symbol,
            OrderSide side,
            BigDecimal executionPrice,
            BigDecimal executionQty,
            BigDecimal quoteAmount,
            Instant executedAt
    ) {
        this.executionId = Objects.requireNonNull(executionId, "executionId is null");
        this.orderId = Objects.requireNonNull(orderId, "orderId is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.symbol = Objects.requireNonNull(symbol, "symbol is null");
        this.side = Objects.requireNonNull(side, "side is null");
        this.executionPrice = requirePositive(executionPrice, "executionPrice");
        this.executionQty = requirePositive(executionQty, "executionQty");
        this.quoteAmount = requirePositive(quoteAmount, "quoteAmount");
        this.executedAt = Objects.requireNonNull(executedAt, "executedAt is null");
    }

    public static Execution create(
            ExecutionId executionId,
            OrderId orderId,
            AccountId accountId,
            Symbol symbol,
            OrderSide side,
            BigDecimal executionPrice,
            BigDecimal executionQty,
            BigDecimal quoteAmount,
            Instant executedAt
    ) {
        return new Execution(
                executionId,
                orderId,
                accountId,
                symbol,
                side,
                executionPrice,
                executionQty,
                quoteAmount,
                executedAt
        );
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice;
    }

    public BigDecimal getExecutionQty() {
        return executionQty;
    }

    public BigDecimal getQuoteAmount() {
        return quoteAmount;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is null");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value.stripTrailingZeros();
    }
}
