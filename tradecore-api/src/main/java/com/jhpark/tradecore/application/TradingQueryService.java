package com.jhpark.tradecore.application;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ResourceNotFoundException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.ExecutionQueryRepository;
import com.jhpark.tradecore.core.application.port.out.OrderQueryRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.application.query.OrderSearchCondition;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TradingQueryService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final ExecutionQueryRepository executionQueryRepository;

    public TradingQueryService(
            AccountRepository accountRepository,
            OrderRepository orderRepository,
            OrderQueryRepository orderQueryRepository,
            ExecutionQueryRepository executionQueryRepository
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository is null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is null");
        this.orderQueryRepository = Objects.requireNonNull(orderQueryRepository, "orderQueryRepository is null");
        this.executionQueryRepository = Objects.requireNonNull(executionQueryRepository, "executionQueryRepository is null");
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(new AccountId(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다. accountId=" + accountId));
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }

    public PageResult<OrderSummary> getOrders(
            String accountId,
            String symbol,
            String status,
            String side,
            int page,
            int size
    ) {
        accountRepository.findById(new AccountId(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다. accountId=" + accountId));

        return orderQueryRepository.search(
                new OrderSearchCondition(
                        accountId,
                        symbol,
                        status,
                        side,
                        page,
                        size
                )
        );
    }

    public List<ExecutionSummary> getExecutionsByOrder(String orderId) {
        orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. orderId=" + orderId));

        return executionQueryRepository.findByOrderId(new OrderId(orderId));
    }

    public PageResult<ExecutionSummary> getExecutionsByAccount(
            String accountId,
            int page,
            int size
    ) {
        accountRepository.findById(new AccountId(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("계정을 찾을 수 없습니다. accountId=" + accountId));

        return executionQueryRepository.findByAccountId(new AccountId(accountId), page, size);
    }
}
