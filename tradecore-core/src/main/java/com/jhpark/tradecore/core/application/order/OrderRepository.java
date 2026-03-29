package com.jhpark.tradecore.core.application.order;

import com.jhpark.tradecore.core.order.Order;

public interface OrderRepository {
    Order save(Order order);
}
