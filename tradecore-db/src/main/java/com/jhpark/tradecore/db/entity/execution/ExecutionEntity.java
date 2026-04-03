package com.jhpark.tradecore.db.entity.execution;

import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "executions")
public class ExecutionEntity {

    @Id
    @Column(name = "execution_id", nullable = false, updatable = false, length = 100)
    private String executionId;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_asset", nullable = false, length = 20)
    private Asset baseAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_asset", nullable = false, length = 20)
    private Asset quoteAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 20)
    private OrderSide side;

    @Column(name = "execution_price", nullable = false, precision = 30, scale = 12)
    private BigDecimal executionPrice;

    @Column(name = "execution_qty", nullable = false, precision = 30, scale = 12)
    private BigDecimal executionQty;

    @Column(name = "quote_amount", nullable = false, precision = 30, scale = 12)
    private BigDecimal quoteAmount;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected ExecutionEntity() {
    }

    public ExecutionEntity(
            String executionId,
            String orderId,
            String accountId,
            Asset baseAsset,
            Asset quoteAsset,
            OrderSide side,
            BigDecimal executionPrice,
            BigDecimal executionQty,
            BigDecimal quoteAmount,
            Instant executedAt
    ) {
        this.executionId = Objects.requireNonNull(executionId, "executionId is null");
        this.orderId = Objects.requireNonNull(orderId, "orderId is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.baseAsset = Objects.requireNonNull(baseAsset, "baseAsset is null");
        this.quoteAsset = Objects.requireNonNull(quoteAsset, "quoteAsset is null");
        this.side = Objects.requireNonNull(side, "side is null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "executionPrice is null");
        this.executionQty = Objects.requireNonNull(executionQty, "executionQty is null");
        this.quoteAmount = Objects.requireNonNull(quoteAmount, "quoteAmount is null");
        this.executedAt = Objects.requireNonNull(executedAt, "executedAt is null");
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public Asset getBaseAsset() {
        return baseAsset;
    }

    public Asset getQuoteAsset() {
        return quoteAsset;
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
}