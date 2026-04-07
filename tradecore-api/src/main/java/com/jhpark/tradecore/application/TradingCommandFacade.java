package com.jhpark.tradecore.application;

/*
    repository adapter에 @Transactional이 있지만
    실제로는 ApplyExecutionService.apply() 안에서 하나의 트랜잭션으로 묶여야 함
*/

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.order.ApplyExecutionCommand;
import com.jhpark.tradecore.core.application.order.ApplyExecutionResult;
import com.jhpark.tradecore.core.application.order.ApplyExecutionService;
import com.jhpark.tradecore.core.application.order.CancelOrderCommand;
import com.jhpark.tradecore.core.application.order.CancelOrderResult;
import com.jhpark.tradecore.core.application.order.CancelOrderService;
import com.jhpark.tradecore.core.application.order.PlaceOrderCommand;
import com.jhpark.tradecore.core.application.order.PlaceOrderResult;
import com.jhpark.tradecore.core.application.order.PlaceOrderService;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@Transactional
public class TradingCommandFacade {

    private final PlaceOrderService placeOrderService;
    private final CancelOrderService cancelOrderService;
    private final ApplyExecutionService applyExecutionService;

    public TradingCommandFacade(
            PlaceOrderService placeOrderService,
            CancelOrderService cancelOrderService,
            ApplyExecutionService applyExecutionService
    ) {
        this.placeOrderService = Objects.requireNonNull(placeOrderService, "placeOrderService is null");
        this.cancelOrderService = Objects.requireNonNull(cancelOrderService, "cancelOrderService is null");
        this.applyExecutionService = Objects.requireNonNull(applyExecutionService, "applyExecutionService is null");
    }

    public PlaceOrderResult placeOrder(
            String accountId,
            String symbol,
            String side,
            String orderType,
            BigDecimal price,
            BigDecimal qty
    ) {
        PlaceOrderCommand command = new PlaceOrderCommand(
                new AccountId(accountId),
                parseSymbol(symbol),
                OrderSide.valueOf(side),
                OrderType.valueOf(orderType),
                price,
                qty
        );

        return placeOrderService.place(command);
    }

    public CancelOrderResult cancelOrder(
            String accountId,
            String orderId
    ) {
        CancelOrderCommand command = new CancelOrderCommand(
                new AccountId(accountId),
                new OrderId(orderId)
        );

        return cancelOrderService.cancel(command);
    }

    public ApplyExecutionResult applyExecution(
            String executionId,
            String orderId,
            BigDecimal executionPrice,
            BigDecimal executionQty
    ) {
        ApplyExecutionCommand command = new ApplyExecutionCommand(
                new ExecutionId(executionId),
                new OrderId(orderId),
                executionPrice,
                executionQty
        );

        return applyExecutionService.apply(command);
    }

    private Symbol parseSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }

        String normalized = symbol.trim().toUpperCase();

        if (normalized.endsWith("USDT")) {
            String baseCode = normalized.substring(0, normalized.length() - 4);
            Asset baseAsset = Asset.valueOf(baseCode);
            return new Symbol(baseAsset, Asset.USDT);
        }

        throw new IllegalArgumentException("지원하지 않는 symbol 형식입니다. symbol=" + symbol);
    }
}
