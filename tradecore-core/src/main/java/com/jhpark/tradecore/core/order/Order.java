package com.jhpark.tradecore.core.order;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.market.Symbol;

import java.math.BigDecimal;
import java.util.Objects;

public final class Order {

    private final OrderId orderId;
    private final AccountId accountId;
    private final Symbol symbol;
    private final OrderSide side;
    private final OrderType type;
    private final OrderStatus status;
    private final BigDecimal price;
    private final BigDecimal qty;
    private final BigDecimal filledQty;

    private Order(
        OrderId orderId,
        AccountId accountId,
        Symbol symbol,
        OrderSide side,
        OrderType type,
        OrderStatus status,
        BigDecimal price,
        BigDecimal qty,
        BigDecimal filledQty
    ) {
        this.orderId = Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");
        this.accountId = Objects.requireNonNull(accountId, "계정 ID는 null일 수 없습니다.");
        this.symbol = Objects.requireNonNull(symbol, "심볼은 null일 수 없습니다.");
        this.side = Objects.requireNonNull(side, "주문 방향은 null일 수 없습니다.");
        this.type = Objects.requireNonNull(type, "주문 타입은 null일 수 없습니다.");
        this.status = Objects.requireNonNull(status, "주문 상태는 null일 수 없습니다.");
        this.price = normalizeNullable(price);
        this.qty = positive(qty, "주문 수량");
        this.filledQty = nonNegative(filledQty, "체결 수량");

        validatePriceByOrderType(this.type, this.price);

        if (this.filledQty.compareTo(this.qty) > 0) {
            throw new IllegalArgumentException("체결 수량은 주문 수량보다 클 수 없습니다.");
        }
    }

    // 지정가 주문 생성
    public static Order newLimitOrder(
            OrderId orderId,
            AccountId accountId,
            Symbol symbol,
            OrderSide side,
            BigDecimal price,
            BigDecimal qty
    ) {
        return new Order(
                orderId,
                accountId,
                symbol,
                side,
                OrderType.LIMIT,
                OrderStatus.NEW,
                price,
                qty,
                BigDecimal.ZERO
        );
    }

    // 시장가 주문 생성
    public static Order newMarketOrder(
            OrderId orderId,
            AccountId accountId,
            Symbol symbol,
            OrderSide side,
            BigDecimal qty
    ) {
        return new Order(
                orderId,
                accountId,
                symbol,
                side,
                OrderType.MARKET,
                OrderStatus.NEW,
                null,
                qty,
                BigDecimal.ZERO
        );
    }

    public Order cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다. status=" + status);
        }

        return new Order(
                this.orderId,
                this.accountId,
                this.symbol,
                this.side,
                this.type,
                OrderStatus.CANCELLED,
                this.price,
                this.qty,
                this.filledQty
        );
    }

    public Order applyFill(BigDecimal fillQty) {
        BigDecimal normalizedFillQty = positive(fillQty, "fillQty");

        if (isClosed()) {
            throw new IllegalStateException("종결된 주문에는 체결을 적용할 수 없습니다. status=" + status);
        }

        BigDecimal nextFilledQty = this.filledQty.add(normalizedFillQty).stripTrailingZeros();

        if (nextFilledQty.compareTo(this.qty) > 0) {
            throw new IllegalArgumentException("누적 체결 수량은 주문 수량보다 클 수 없습니다.");
        }

        OrderStatus nextStatus = determineStatus(nextFilledQty, this.qty);

        return new Order(
                this.orderId,
                this.accountId,
                this.symbol,
                this.side,
                this.type,
                nextStatus,
                this.price,
                this.qty,
                nextFilledQty
        );
    }

    public boolean canCancel() {
        return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED;
    }

    public boolean isClosed() {
        return status == OrderStatus.FILLED || status == OrderStatus.CANCELLED;
    }

    // 미체결 수량 반환
    public BigDecimal remainingQty() {
        return this.qty.subtract(this.filledQty);
    }

    public boolean isBuy() {
        return side == OrderSide.BUY;
    }

    public boolean isSell() {
        return side == OrderSide.SELL;
    }

    public boolean isLimitOrder() {
        return type == OrderType.LIMIT;
    }

    public boolean isMarketOrder() {
        return type == OrderType.MARKET;
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

    private static void validatePriceByOrderType(OrderType type, BigDecimal price) {
        if (type == OrderType.LIMIT && price == null) {
            throw new IllegalArgumentException("지정가 주문은 가격이 반드시 필요합니다.");
        }

        if (type == OrderType.MARKET && price != null) {
            throw new IllegalArgumentException("시장가 주문은 가격을 가질 수 없습니다.");
        }
    }

    private static BigDecimal normalizeNullable(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private static BigDecimal positive(BigDecimal value, String fieldName) {
        BigDecimal normalized = nonNegative(value, fieldName);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다.");
        }
        return normalized;
    }

    private static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) null일 수 없습니다.");
        }

        BigDecimal normalized = value.stripTrailingZeros();

        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0 이상이어야 합니다.");
        }

        return normalized;
    }

    private static OrderStatus determineStatus(BigDecimal filledQty, BigDecimal qty) {
        if (filledQty.compareTo(BigDecimal.ZERO) == 0) {
            return OrderStatus.NEW;
        }

        if (filledQty.compareTo(qty) == 0) {
            return OrderStatus.FILLED;
        }

        return OrderStatus.PARTIALLY_FILLED;
    }
}
