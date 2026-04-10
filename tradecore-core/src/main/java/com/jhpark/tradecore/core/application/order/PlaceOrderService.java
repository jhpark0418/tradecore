package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderType;

import java.math.BigDecimal;
import java.util.Objects;

public class PlaceOrderService {
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public PlaceOrderService(AccountRepository accountRepository, OrderRepository orderRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
    }

    public PlaceOrderResult place(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command is null");

        validate(command);

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다. accountId=" + command.accountId().value()));

        Asset lockAsset = calculateLockAsset(command);
        BigDecimal lockAmount = calculateLockAmount(command);

        Account lockedAccount = account.lock(lockAsset, lockAmount);

        Order order = Order.newLimitOrder(
                OrderId.newId(),
                command.accountId(),
                command.symbol(),
                command.side(),
                command.price(),
                command.qty()
        );

        Account savedAccount = accountRepository.save(lockedAccount);
        Order savedOrder = orderRepository.save(order);

        return new PlaceOrderResult(savedAccount, savedOrder);
    }

    private void validate(PlaceOrderCommand command) {
        Objects.requireNonNull(command.accountId(), "accountId is null");
        Objects.requireNonNull(command.symbol(), "symbol is null");
        Objects.requireNonNull(command.side(), "side is null");
        Objects.requireNonNull(command.orderType(), "orderType is null");
        requirePositive(command.qty(), "qty");

        if (command.orderType() != OrderType.LIMIT) {
            throw new UnsupportedOperationException("현재 단계에서는 LIMIT 주문만 지원합니다.");
        }

        requirePositive(command.price(), "price");
    }

    private Asset calculateLockAsset(PlaceOrderCommand command) {
        if (command.side() == OrderSide.BUY) {
            return command.symbol().quoteAsset();
        }
        return command.symbol().baseAsset();
    }

    private BigDecimal calculateLockAmount(PlaceOrderCommand command) {
        if (command.side() == OrderSide.BUY) {
            return command.price().multiply(command.qty()).stripTrailingZeros();
        }
        return command.qty().stripTrailingZeros();
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
