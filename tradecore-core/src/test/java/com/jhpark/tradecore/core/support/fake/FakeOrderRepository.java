package com.jhpark.tradecore.core.support.fake;

import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeOrderRepository implements OrderRepository {

    private final Map<String, Order> storage = new HashMap<>();
    private boolean simulateConcurrentUpdateOnNextSave;

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(storage.get(orderId.value()));
    }

    @Override
    public Order save(Order order) {
        Order current = storage.get(order.getOrderId().value());

        if (current == null) {
            if (order.getVersion() != 0L) {
                throw new ConcurrencyConflictException(
                        "New order must start with version 0. orderId=" + order.getOrderId().value()
                );
            }

            Order persisted = order.withVersion(1L);
            storage.put(order.getOrderId().value(), persisted);
            return persisted;
        }

        if (simulateConcurrentUpdateOnNextSave) {
            simulateConcurrentUpdateOnNextSave = false;
            Order bumped = current.withVersion(current.getVersion() + 1);
            storage.put(order.getOrderId().value(), bumped);
            current = bumped;
        }

        if (order.getVersion() != current.getVersion()) {
            throw new ConcurrencyConflictException(
                    "Order version conflict occurred for orderId=" + order.getOrderId().value()
                            + ", expected=" + current.getVersion()
                            + ", actual=" + order.getVersion()
            );
        }

        Order persisted = order.withVersion(current.getVersion() + 1);
        storage.put(order.getOrderId().value(), persisted);
        return persisted;
    }

    public void simulateConcurrentUpdateOnNextSave() {
        this.simulateConcurrentUpdateOnNextSave = true;
    }

    public void clear() {
        storage.clear();
    }

    public int size() {
        return storage.size();
    }
}
