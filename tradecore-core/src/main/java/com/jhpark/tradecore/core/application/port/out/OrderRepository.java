package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.order.Order;
import com.jhpark.tradecore.core.order.OrderId;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    Order save(Order order);
}
