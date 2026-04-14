package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.entity.execution.ExecutionEntity;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExecutionQueryRepositoryAdapter.class)
@ContextConfiguration(classes = DbTestApplication.class)
class ExecutionQueryRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private ExecutionQueryRepositoryAdapter executionQueryRepositoryAdapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByOrderId_returnsExecutionsSortedByExecutedAtAsc() {
        persistExecution(
                "exec-002",
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("70100"),
                new BigDecimal("0.2"),
                new BigDecimal("14020"),
                Instant.parse("2026-04-13T10:05:00Z")
        );

        persistExecution(
                "exec-001",
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.1"),
                new BigDecimal("7000"),
                Instant.parse("2026-04-13T10:00:00Z")
        );

        persistExecution(
                "exec-999",
                "order-999",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                new BigDecimal("3200"),
                new BigDecimal("1"),
                new BigDecimal("3200"),
                Instant.parse("2026-04-13T09:00:00Z")
        );

        List<ExecutionSummary> result = executionQueryRepositoryAdapter.findByOrderId(new OrderId("order-001"));

        assertEquals(2, result.size());

        assertEquals("exec-001", result.get(0).executionId());
        assertEquals("order-001", result.get(0).orderId());
        assertEquals("account-1", result.get(0).accountId());
        assertEquals("BTCUSDT", result.get(0).symbol());
        assertEquals(OrderSide.BUY, result.get(0).side());
        assertEquals(0, result.get(0).executionPrice().compareTo(new BigDecimal("70000")));
        assertEquals(0, result.get(0).executionQty().compareTo(new BigDecimal("0.1")));
        assertEquals(0, result.get(0).quoteAmount().compareTo(new BigDecimal("7000")));
        assertEquals(Instant.parse("2026-04-13T10:00:00Z"), result.get(0).executedAt());

        assertEquals("exec-002", result.get(1).executionId());
        assertEquals(Instant.parse("2026-04-13T10:05:00Z"), result.get(1).executedAt());
    }

    @Test
    void findByAccountId_appliesPagingAndSortsByExecutedAtDesc() {
        persistExecution(
                "exec-001",
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.1"),
                new BigDecimal("7000"),
                Instant.parse("2026-04-13T10:00:00Z")
        );

        persistExecution(
                "exec-002",
                "order-002",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                new BigDecimal("3500"),
                new BigDecimal("1.2"),
                new BigDecimal("4200"),
                Instant.parse("2026-04-13T11:00:00Z")
        );

        persistExecution(
                "exec-003",
                "order-003",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("70200"),
                new BigDecimal("0.05"),
                new BigDecimal("3510"),
                Instant.parse("2026-04-13T12:00:00Z")
        );

        persistExecution(
                "exec-999",
                "order-999",
                "account-2",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("69900"),
                new BigDecimal("0.3"),
                new BigDecimal("20970"),
                Instant.parse("2026-04-13T13:00:00Z")
        );

        PageResult<ExecutionSummary> result = executionQueryRepositoryAdapter.findByAccountId(
                new AccountId("account-1"),
                0,
                2
        );

        assertEquals(2, result.content().size());
        assertEquals(3L, result.totalElements());
        assertEquals(2, result.totalPages());
        assertTrue(result.hasNext());

        assertEquals("exec-003", result.content().get(0).executionId());
        assertEquals("exec-002", result.content().get(1).executionId());
    }

    @Test
    void findByAccountId_returnsOnlyExecutionsOfRequestedAccount() {
        persistExecution(
                "exec-001",
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                new BigDecimal("70000"),
                new BigDecimal("0.1"),
                new BigDecimal("7000"),
                Instant.parse("2026-04-13T10:00:00Z")
        );

        persistExecution(
                "exec-002",
                "order-002",
                "account-2",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                new BigDecimal("3500"),
                new BigDecimal("2"),
                new BigDecimal("7000"),
                Instant.parse("2026-04-13T11:00:00Z")
        );

        PageResult<ExecutionSummary> result = executionQueryRepositoryAdapter.findByAccountId(
                new AccountId("account-1"),
                0,
                10
        );

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());

        assertEquals("exec-001", result.content().getFirst().executionId());
        assertEquals("account-1", result.content().getFirst().accountId());
    }

    private void persistExecution(
            String executionId,
            String orderId,
            String accountId,
            Asset baseAsset,
            Asset quoteAsset,
            OrderSide side,
            BigDecimal executionPrice,
            BigDecimal executionQty,
            BigDecimal quoteAmount,
            Instant executedAt
    ) {
        ExecutionEntity entity = new ExecutionEntity(
                executionId,
                orderId,
                accountId,
                baseAsset,
                quoteAsset,
                side,
                executionPrice,
                executionQty,
                quoteAmount,
                executedAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();
    }
}