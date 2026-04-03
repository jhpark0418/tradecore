package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ContextConfiguration(classes = DbTestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExecutionRepositoryAdapter.class)
class ExecutionRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private ExecutionRepositoryAdapter executionRepositoryAdapter;

    @Test
    void saveAndFindById() {
        Execution execution = Execution.create(
                new ExecutionId("exec-1"),
                new OrderId("order-1"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.1"),
                new BigDecimal("7000"),
                Instant.parse("2026-04-03T10:15:30Z")
        );

        Execution saved = executionRepositoryAdapter.save(execution);
        Optional<Execution> found = executionRepositoryAdapter.findById(new ExecutionId("exec-1"));

        assertEquals("exec-1", saved.getExecutionId().value());
        assertTrue(found.isPresent());
        assertEquals("order-1", found.get().getOrderId().value());
        assertEquals("account-1", found.get().getAccountId().value());
        assertEquals(OrderSide.BUY, found.get().getSide());
        assertEquals(0, found.get().getExecutionPrice().compareTo(new BigDecimal("70000")));
        assertEquals(0, found.get().getExecutionQty().compareTo(new BigDecimal("0.1")));
        assertEquals(0, found.get().getQuoteAmount().compareTo(new BigDecimal("7000")));
        assertEquals(Instant.parse("2026-04-03T10:15:30Z"), found.get().getExecutedAt());
    }

    @Test
    void duplicateExecutionIdThrowsConcurrencyConflict() {
        Execution execution = Execution.create(
                new ExecutionId("exec-dup"),
                new OrderId("order-2"),
                new AccountId("account-1"),
                new Symbol(Asset.BTC, Asset.USDT),
                OrderSide.SELL,
                new BigDecimal("71000"),
                new BigDecimal("0.2"),
                new BigDecimal("14200"),
                Instant.parse("2026-04-03T10:20:00Z")
        );

        executionRepositoryAdapter.save(execution);

        ConcurrencyConflictException exception = assertThrows(
                ConcurrencyConflictException.class,
                () -> executionRepositoryAdapter.save(execution)
        );

        assertTrue(exception.getMessage().contains("exec-dup"));
    }
}