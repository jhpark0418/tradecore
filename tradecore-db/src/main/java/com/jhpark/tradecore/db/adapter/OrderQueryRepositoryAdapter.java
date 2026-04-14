package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.application.port.out.OrderQueryRepository;
import com.jhpark.tradecore.core.application.query.OrderSearchCondition;
import com.jhpark.tradecore.core.application.query.OrderSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.market.SymbolParser;
import com.jhpark.tradecore.core.order.OrderSide;
import com.jhpark.tradecore.core.order.OrderStatus;
import com.jhpark.tradecore.db.entity.order.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class OrderQueryRepositoryAdapter implements OrderQueryRepository {

    private final EntityManager entityManager;

    public OrderQueryRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public PageResult<OrderSummary> search(OrderSearchCondition condition) {
        Map<String, Object> params = new HashMap<>();

        StringBuilder where = new StringBuilder(" where o.accountId = :accountId");
        params.put("accountId", condition.accountId());

        if (hasText(condition.symbol())) {
            Symbol symbol = SymbolParser.parse(condition.symbol());
            where.append(" and o.baseAsset = :baseAsset and o.quoteAsset = :quoteAsset");
            params.put("baseAsset", symbol.baseAsset());
            params.put("quoteAsset", symbol.quoteAsset());
        }

        if (hasText(condition.status())) {
            where.append(" and o.status = :status");
            params.put("status", OrderStatus.valueOf(condition.status().trim().toUpperCase(Locale.ROOT)));
        }

        if (hasText(condition.side())) {
            where.append(" and o.side = :side");
            params.put("side", OrderSide.valueOf(condition.side().trim().toUpperCase(Locale.ROOT)));
        }

        String contentJpql = """
                select o
                from OrderEntity o
                """ + where + " order by o.createdAt desc, o.orderId desc";

        String countJpql = """
                select count(o)
                from OrderEntity o
                """ + where;

        TypedQuery<OrderEntity> contentQuery = entityManager.createQuery(contentJpql, OrderEntity.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        applyParams(contentQuery, params);
        applyParams(countQuery, params);

        contentQuery.setFirstResult(condition.page() * condition.size());
        contentQuery.setMaxResults(condition.size());

        List<OrderSummary> content = contentQuery.getResultList()
                .stream()
                .map(this::toSummary)
                .toList();

        long totalElements = countQuery.getSingleResult();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / condition.size());

        boolean hasNext = (long) (condition.page() + 1) * condition.size() < totalElements;

        return new PageResult<>(
                content,
                condition.page(),
                condition.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    private void applyParams(jakarta.persistence.Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private OrderSummary toSummary(OrderEntity entity) {
        return new OrderSummary(
                entity.getOrderId(),
                entity.getAccountId(),
                entity.getBaseAsset().name() + entity.getQuoteAsset().name(),
                entity.getSide().name(),
                entity.getType().name(),
                entity.getStatus().name(),
                entity.getPrice(),
                entity.getQty(),
                entity.getFilledQty(),
                entity.getQty().subtract(entity.getFilledQty()),
                entity.getVersion() == null ? 0L : entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
