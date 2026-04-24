package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.application.query.OrderSearchCondition;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.core.order.OrderType;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.entity.order.OrderEntity;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderQueryRepositoryAdapter.class)
@ContextConfiguration(classes = DbTestApplication.class)
class OrderQueryRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private OrderQueryRepositoryAdapter orderQueryRepositoryAdapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void search_returnsOrdersByAccount_sortedByCreatedAtDesc() {
        persistOrder(
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-002",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.PARTIALLY_FILLED,
                new BigDecimal("3200"),
                new BigDecimal("2"),
                new BigDecimal("0.5"),
                OffsetDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-003",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.CANCELLED,
                new BigDecimal("41000"),
                new BigDecimal("0.2"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 8, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-999",
                "account-2",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("40000"),
                new BigDecimal("1"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 11, 0, 0, 0, ZoneOffset.UTC)
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(
                new OrderSearchCondition("account-1", null, null, null, 0, 10)
        );

        assertEquals(3, result.content().size());
        assertEquals(3L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());

        assertEquals("order-002", result.content().get(0).orderId());
        assertEquals("order-001", result.content().get(1).orderId());
        assertEquals("order-003", result.content().get(2).orderId());
    }

    @Test
    void search_appliesStatusSideAndSymbolFilters() {
        persistOrder(
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-002",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.NEW,
                new BigDecimal("42100"),
                new BigDecimal("0.7"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-003",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("3000"),
                new BigDecimal("1"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 11, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-004",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.CANCELLED,
                new BigDecimal("41900"),
                new BigDecimal("0.4"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(
                new OrderSearchCondition("account-1", "BTCUSDT", "NEW", "BUY", 0, 10)
        );

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());

        OrderSummary summary = result.content().getFirst();
        assertEquals("order-001", summary.orderId());
        assertEquals("BTCUSDT", summary.symbol());
        assertEquals("BUY", summary.side());
        assertEquals("NEW", summary.status());
    }

    @Test
    void search_appliesPagination() {
        persistOrder(
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.1"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-002",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42100"),
                new BigDecimal("0.2"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-003",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42200"),
                new BigDecimal("0.3"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 11, 0, 0, 0, ZoneOffset.UTC)
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(
                new OrderSearchCondition("account-1", null, null, null, 0, 2)
        );

        assertEquals(2, result.content().size());
        assertEquals(3L, result.totalElements());
        assertEquals(2, result.totalPages());
        assertTrue(result.hasNext());

        assertEquals("order-003", result.content().get(0).orderId());
        assertEquals("order-002", result.content().get(1).orderId());
    }

    @Test
    void search_appliesCreatedRangeFilter() {
        persistOrder(
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("70000"),
                new BigDecimal("0.1"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-01T00:00:00Z")
        );

        persistOrder(
                "order-002",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("71000"),
                new BigDecimal("0.2"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-10T00:00:00Z")
        );

        persistOrder(
                "order-003",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("72000"),
                new BigDecimal("0.3"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-20T00:00:00Z")
        );

        OrderSearchCondition condition = new OrderSearchCondition(
                "account-1",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-05T00:00:00Z"),
                OffsetDateTime.parse("2026-04-15T23:59:59Z"),
                0,
                20
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(condition);

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
        assertEquals("order-002", result.content().get(0).orderId());
    }

    @Test
    void search_appliesCreatedFromOnly() {
        persistOrder(
                "order-011",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.NEW,
                new BigDecimal("3000"),
                new BigDecimal("1"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-01T00:00:00Z")
        );

        persistOrder(
                "order-012",
                "account-1",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.NEW,
                new BigDecimal("3200"),
                new BigDecimal("1"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-18T00:00:00Z")
        );

        OrderSearchCondition condition = new OrderSearchCondition(
                "account-1",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-10T00:00:00Z"),
                null,
                0,
                20
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(condition);

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
        assertEquals("order-012", result.content().get(0).orderId());
    }

    @Test
    void search_returnsAllOrders_whenAccountIdIsNull() {
        persistOrder(
                "order-001",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-002",
                "account-2",
                Asset.ETH,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.PARTIALLY_FILLED,
                new BigDecimal("3200"),
                new BigDecimal("2"),
                new BigDecimal("0.5"),
                OffsetDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneOffset.UTC)
        );

        persistOrder(
                "order-003",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.CANCELLED,
                new BigDecimal("41000"),
                new BigDecimal("0.2"),
                BigDecimal.ZERO,
                OffsetDateTime.of(2026, 4, 13, 8, 0, 0, 0, ZoneOffset.UTC)
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(
                new OrderSearchCondition(null, null, null, null, 0, 10)
        );

        assertEquals(3, result.content().size());
        assertEquals(3L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());

        assertEquals("order-002", result.content().get(0).orderId());
        assertEquals("account-2", result.content().get(0).accountId());

        assertEquals("order-001", result.content().get(1).orderId());
        assertEquals("account-1", result.content().get(1).accountId());

        assertEquals("order-003", result.content().get(2).orderId());
        assertEquals("account-1", result.content().get(2).accountId());
    }

    @Test
    void search_appliesFilters_whenAccountIdIsNull() {
        persistOrder(
                "order-101",
                "account-1",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42000"),
                new BigDecimal("0.5"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-13T09:00:00Z")
        );

        persistOrder(
                "order-102",
                "account-2",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42100"),
                new BigDecimal("0.7"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-13T10:00:00Z")
        );

        // symbol mismatch
        persistOrder(
                "order-103",
                "account-3",
                Asset.ETH,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("3200"),
                new BigDecimal("1"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-13T11:00:00Z")
        );

        // side mismatch
        persistOrder(
                "order-104",
                "account-4",
                Asset.BTC,
                Asset.USDT,
                OrderSide.SELL,
                OrderStatus.NEW,
                new BigDecimal("42200"),
                new BigDecimal("0.8"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-13T12:00:00Z")
        );

        // status mismatch
        persistOrder(
                "order-105",
                "account-5",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.CANCELLED,
                new BigDecimal("42300"),
                new BigDecimal("0.9"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-13T13:00:00Z")
        );

        // created range mismatch
        persistOrder(
                "order-106",
                "account-6",
                Asset.BTC,
                Asset.USDT,
                OrderSide.BUY,
                OrderStatus.NEW,
                new BigDecimal("42400"),
                new BigDecimal("1.0"),
                BigDecimal.ZERO,
                OffsetDateTime.parse("2026-04-01T00:00:00Z")
        );

        OrderSearchCondition condition = new OrderSearchCondition(
                null,
                "BTCUSDT",
                "NEW",
                "BUY",
                OffsetDateTime.parse("2026-04-13T00:00:00Z"),
                OffsetDateTime.parse("2026-04-13T23:59:59Z"),
                0,
                10
        );

        PageResult<OrderSummary> result = orderQueryRepositoryAdapter.search(condition);

        assertEquals(2, result.content().size());
        assertEquals(2L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());

        assertEquals("order-102", result.content().get(0).orderId());
        assertEquals("account-2", result.content().get(0).accountId());

        assertEquals("order-101", result.content().get(1).orderId());
        assertEquals("account-1", result.content().get(1).accountId());
    }

    private void persistOrder(
            String orderId,
            String accountId,
            Asset baseAsset,
            Asset quoteAsset,
            OrderSide side,
            OrderStatus status,
            BigDecimal price,
            BigDecimal qty,
            BigDecimal filledQty,
            OffsetDateTime createdAt
    ) {
        OrderEntity entity = new OrderEntity(
                orderId,
                accountId,
                baseAsset,
                quoteAsset,
                side,
                OrderType.LIMIT,
                status,
                price,
                qty,
                filledQty,
                null
        );

        entityManager.persist(entity);
        entityManager.flush();

        entityManager.createNativeQuery("""
                    update orders
                       set created_at = :createdAt,
                           updated_at = :updatedAt
                     where order_id = :orderId
                    """)
                .setParameter("createdAt", Timestamp.from(createdAt.toInstant()))
                .setParameter("updatedAt", Timestamp.from(createdAt.toInstant()))
                .setParameter("orderId", orderId)
                .executeUpdate();

        entityManager.clear();
    }
}