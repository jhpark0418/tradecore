package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.db.entity.order.OrderEntity;
import com.jhpark.tradecore.db.repository.OrderJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly=true)
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity current = orderJpaRepository.findById(order.getOrderId().value()).orElse(null);

        if (current == null) {
            if (order.getVersion() != 0L) {
                throw new ConcurrencyConflictException(
                        "New order must start with version 0. orderId=" + order.getOrderId().value()
                );
            }

            OrderEntity created = new OrderEntity(
                    order.getOrderId().value(),
                    order.getAccountId().value(),
                    order.getSymbol().baseAsset(),
                    order.getSymbol().quoteAsset(),
                    order.getSide(),
                    order.getType(),
                    order.getStatus(),
                    order.getPrice(),
                    order.getQty(),
                    order.getFilledQty(),
                    null
            );

            OrderEntity saved = orderJpaRepository.saveAndFlush(created);
            return toDomain(saved);
        }

        long currentVersion = current.getVersion() == null ? 0L : current.getVersion();
        if (order.getVersion() != currentVersion) {
            throw new ConcurrencyConflictException(
                    "Order version conflict occurred for orderId=" + order.getOrderId().value()
                            + ", expected=" + currentVersion
                            + ", actual=" + order.getVersion()
            );
        }

        current.updateFrom(
                order.getStatus(),
                order.getPrice(),
                order.getQty(),
                order.getFilledQty()
        );

        OrderEntity saved = orderJpaRepository.saveAndFlush(current);
        return toDomain(saved);
    }

    private OrderEntity toEntity(Order order) {
        Long version = order.getVersion() == 0L ? null : order.getVersion();

        return new OrderEntity(
                order.getOrderId().value(),
                order.getAccountId().value(),
                order.getSymbol().baseAsset(),
                order.getSymbol().quoteAsset(),
                order.getSide(),
                order.getType(),
                order.getStatus(),
                order.getPrice(),
                order.getQty(),
                order.getFilledQty(),
                version
        );
    }

    private Order toDomain(OrderEntity entity) {
        long version = entity.getVersion() == null ? 0L : entity.getVersion();

        return Order.restore(
                new OrderId(entity.getOrderId()),
                new AccountId(entity.getAccountId()),
                new Symbol(entity.getBaseAsset(), entity.getQuoteAsset()),
                entity.getSide(),
                entity.getType(),
                entity.getStatus(),
                entity.getPrice(),
                entity.getQty(),
                entity.getFilledQty(),
                version
        );
    }
}
