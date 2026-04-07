package com.jhpark.tradecore.application;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TradingQueryService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public TradingQueryService(
            AccountRepository accountRepository,
            OrderRepository orderRepository
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(new AccountId(accountId))
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다. accountId=" + accountId));
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }
}
