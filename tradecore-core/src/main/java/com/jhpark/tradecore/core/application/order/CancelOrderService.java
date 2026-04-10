package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderType;

import java.math.BigDecimal;
import java.util.Objects;

public class CancelOrderService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public CancelOrderService(AccountRepository accountRepository, OrderRepository orderRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
    }

    public CancelOrderResult cancel(CancelOrderCommand command) {
        Objects.requireNonNull(command, "command is null");
        Objects.requireNonNull(command.accountId(), "accountId is null");
        Objects.requireNonNull(command.orderId(), "orderId is null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "주문을 찾을 수 없습니다. orderId=" + command.orderId().value()
                ));

        if (!order.getAccountId().equals(command.accountId())) {
            throw new IllegalArgumentException("주문 소유 계정이 일치하지 않습니다.");
        }

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "계정을 찾을 수 없습니다. accountId=" + command.accountId().value()
                ));

        Asset unlockAsset = calculateUnlockAsset(order);
        BigDecimal unlockAmount = calculateUnlockAmount(order);

        Account unlockedAccount = account.unlock(unlockAsset, unlockAmount);
        Order canceledOrder = order.cancel();

        Account savedAccount = accountRepository.save(unlockedAccount);
        Order savedOrder = orderRepository.save(canceledOrder);

        return new CancelOrderResult(savedAccount, savedOrder);
    }

    private Asset calculateUnlockAsset(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            return order.getSymbol().quoteAsset();
        }
        return order.getSymbol().baseAsset();
    }

    private BigDecimal calculateUnlockAmount(Order order) {
        if (order.getType() != OrderType.LIMIT) {
            throw new UnsupportedOperationException("현재 단계에서는 LIMIT 주문 취소만 지원합니다.");
        }

        BigDecimal remainingQty = order.remainingQty();

        if (order.getSide() == OrderSide.BUY) {
            return order.getPrice().multiply(remainingQty).stripTrailingZeros();
        }

        return remainingQty.stripTrailingZeros();
    }
}
