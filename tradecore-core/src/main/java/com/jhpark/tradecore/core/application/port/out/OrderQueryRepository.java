package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.application.query.OrderSearchCondition;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;

public interface OrderQueryRepository {
    PageResult<OrderSummary> search(OrderSearchCondition condition);
}
