package com.jhpark.tradecore.api.execution.response;

import com.jhpark.tradecore.core.application.order.ApplyExecutionResult;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public record ExecutionResponse(
        String executionId,
        String orderId,
        String accountId,
        String symbol,
        String side,
        BigDecimal executionPrice,
        BigDecimal executionQty,
        BigDecimal quoteAmount,
        Instant executedAt,
        String orderStatus,
        BigDecimal orderFilledQty,
        BigDecimal orderRemainingQty,
        Map<String, BalanceSnapshot> balances
) {
    public static ExecutionResponse from(ApplyExecutionResult result) {
        Map<String, BalanceSnapshot> balanceMap = new TreeMap<>();

        for (Map.Entry<Asset, Balance> entry : result.account().getBalances().entrySet()) {
            Balance balance = entry.getValue();
            balanceMap.put(
                    entry.getKey().name(),
                    new BalanceSnapshot(balance.getAvailable(), balance.getLocked())
            );
        }

        return new ExecutionResponse(
                result.execution().getExecutionId().value(),
                result.execution().getOrderId().value(),
                result.execution().getAccountId().value(),
                result.execution().getSymbol().value(),
                result.execution().getSide().name(),
                result.execution().getExecutionPrice(),
                result.execution().getExecutionQty(),
                result.execution().getQuoteAmount(),
                result.execution().getExecutedAt(),
                result.order().getStatus().name(),
                result.order().getFilledQty(),
                result.order().remainingQty(),
                balanceMap
        );
    }

    public record BalanceSnapshot(
            BigDecimal available,
            BigDecimal locked
    ) {
    }
}
