package com.jhpark.tradecore.db.entity.order;

import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false, length = 100)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "price", precision = 30, scale = 12)
    private BigDecimal price;

    @Column(name = "qty", nullable = false, precision = 30, scale = 12)
    private BigDecimal qty;

    @Column(name = "filled_qty", nullable = false, precision = 30, scale = 12)
    private BigDecimal filledQty;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OrderEntity() {
    }

    public OrderEntity(
            String orderId,
            String accountId,
            Asset baseAsset,
            Asset quoteAsset,
            OrderSide side,
            OrderType type,
            OrderStatus status,
            BigDecimal price,
            BigDecimal qty,
            BigDecimal filledQty,
            Long version
    ) {
        this.orderId = Objects.requireNonNull(orderId, "orderId is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.baseAsset = Objects.requireNonNull(baseAsset, "baseAsset is null");
        this.quoteAsset = Objects.requireNonNull(quoteAsset, "quoteAsset is null");
        this.side = Objects.requireNonNull(side, "side is null");
        this.type = Objects.requireNonNull(type, "type is null");
        this.status = Objects.requireNonNull(status, "status is null");
        this.price = price;
        this.qty = Objects.requireNonNull(qty, "qty is null");
        this.filledQty = Objects.requireNonNull(filledQty, "filledQty is null");
        this.version = version;
    }

    public void updateFrom(
            OrderStatus status,
            BigDecimal price,
            BigDecimal qty,
            BigDecimal filledQty
    ) {
        this.status = Objects.requireNonNull(status);
        this.price = price;
        this.qty = Objects.requireNonNull(qty);
        this.filledQty = Objects.requireNonNull(filledQty);
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

    public OrderType getType() {
        return type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public BigDecimal getFilledQty() {
        return filledQty;
    }

    public Long getVersion() {
        return version;
    }
}