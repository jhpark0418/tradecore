package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.order.OrderId;

import java.util.List;

public interface ExecutionQueryRepository {

    List<ExecutionSummary> findByOrderId(OrderId orderId);

    PageResult<ExecutionSummary> findByAccountId(
            AccountId accountId,
            int page,
            int size
    );
}
