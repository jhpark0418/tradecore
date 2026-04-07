package com.jhpark.tradecore.api.order;

import com.jhpark.tradecore.api.order.request.CancelOrderRequest;
import com.jhpark.tradecore.api.order.response.OrderResponse;
import com.jhpark.tradecore.api.order.request.PlaceOrderRequest;
import com.jhpark.tradecore.application.TradingCommandFacade;
import com.jhpark.tradecore.core.application.order.CancelOrderResult;
import com.jhpark.tradecore.core.application.order.PlaceOrderResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderCommandController {

    private final TradingCommandFacade tradingCommandFacade;

    public OrderCommandController(TradingCommandFacade tradingCommandFacade) {
        this.tradingCommandFacade = tradingCommandFacade;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        PlaceOrderResult result = tradingCommandFacade.placeOrder(
                request.accountId(),
                request.symbol(),
                request.side(),
                request.orderType(),
                request.price(),
                request.qty()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OrderResponse.from(result.order()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        CancelOrderResult result = tradingCommandFacade.cancelOrder(
                request.accountId(),
                orderId
        );

        return ResponseEntity.ok(OrderResponse.from(result.order()));
    }
}
