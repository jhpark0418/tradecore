package com.jhpark.tradecore.infra.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradecore.outbox.topics")
public class OutboxTopicsProperties {

    private String orderPlaced = "tradecore.order.placed";
    private String orderCancelled = "tradecore.order.cancelled";
    private String executionApplied = "tradecore.execution.applied";

    public String getOrderPlaced() {
        return orderPlaced;
    }

    public void setOrderPlaced(String orderPlaced) {
        this.orderPlaced = orderPlaced;
    }

    public String getOrderCancelled() {
        return orderCancelled;
    }

    public void setOrderCancelled(String orderCancelled) {
        this.orderCancelled = orderCancelled;
    }

    public String getExecutionApplied() {
        return executionApplied;
    }

    public void setExecutionApplied(String executionApplied) {
        this.executionApplied = executionApplied;
    }
}
