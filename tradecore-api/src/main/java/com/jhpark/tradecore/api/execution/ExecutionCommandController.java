package com.jhpark.tradecore.api.execution;

import com.jhpark.tradecore.api.execution.request.ApplyExecutionRequest;
import com.jhpark.tradecore.api.execution.response.ExecutionResponse;
import com.jhpark.tradecore.application.TradingCommandFacade;
import com.jhpark.tradecore.core.application.order.ApplyExecutionResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionCommandController {

    private final TradingCommandFacade tradingCommandFacade;

    public ExecutionCommandController(TradingCommandFacade tradingCommandFacade) {
        this.tradingCommandFacade = tradingCommandFacade;
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> applyExecution(
            @Valid @RequestBody ApplyExecutionRequest request
    ) {
        ApplyExecutionResult result = tradingCommandFacade.applyExecution(
                request.executionId(),
                request.orderId(),
                request.executionPrice(),
                request.executionQty()
        );

        return ResponseEntity.ok(ExecutionResponse.from(result));
    }
}
