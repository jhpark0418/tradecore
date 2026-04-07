package com.jhpark.tradecore.config;

import com.jhpark.tradecore.core.application.order.ApplyExecutionService;
import com.jhpark.tradecore.core.application.order.CancelOrderService;
import com.jhpark.tradecore.core.application.order.PlaceOrderService;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.application.port.out.ExecutionRepository;
import com.jhpark.tradecore.core.application.port.out.OrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradeCoreUseCaseConfig {

    @Bean
    public PlaceOrderService placeOrderService(
            AccountRepository accountRepository,
            OrderRepository orderRepository
    ){
        return new PlaceOrderService(accountRepository, orderRepository);
    }

    @Bean
    public CancelOrderService cancelOrderService(
            AccountRepository accountRepository,
            OrderRepository orderRepository
    ) {
        return new CancelOrderService(accountRepository, orderRepository);
    }

    @Bean
    public ApplyExecutionService applyExecutionService(
            AccountRepository accountRepository,
            OrderRepository orderRepository,
            ExecutionRepository executionRepository
    ){
        return new ApplyExecutionService(accountRepository, orderRepository, executionRepository);
    }
}
