package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.port.out.ExecutionQueryRepository;
import com.jhpark.tradecore.core.application.query.ExecutionSummary;
import com.jhpark.tradecore.core.application.query.PageResult;
import com.jhpark.tradecore.core.order.OrderId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ExecutionQueryRepositoryAdapter implements ExecutionQueryRepository {

    private final EntityManager entityManager;

    public ExecutionQueryRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ExecutionSummary> findByOrderId(OrderId orderId) {
        return entityManager.createQuery("""
                select new com.jhpark.tradecore.core.application.query.ExecutionSummary(
                    e.executionId,
                    e.orderId,
                    e.accountId,
                    concat(e.baseAsset, e.quoteAsset),
                    e.side,
                    e.executionPrice,
                    e.executionQty,
                    e.quoteAmount,
                    e.executedAt
                )
                from ExecutionEntity e
                where e.orderId = :orderId
                order by e.executedAt asc, e.executionId asc
                """, ExecutionSummary.class)
                .setParameter("orderId", orderId.value())
                .getResultList();
    }

    @Override
    public PageResult<ExecutionSummary> findByAccountId(AccountId accountId, int page, int size) {
        Long totalElements = entityManager.createQuery("""
                select count(e)
                from ExecutionEntity e
                where e.accountId = :accountId
                """, Long.class)
                .setParameter("accountId", accountId.value())
                .getSingleResult();

        TypedQuery<ExecutionSummary> query = entityManager.createQuery("""
                select new com.jhpark.tradecore.core.application.query.ExecutionSummary(
                    e.executionId,
                    e.orderId,
                    e.accountId,
                    concat(e.baseAsset, e.quoteAsset),
                    e.side,
                    e.executionPrice,
                    e.executionQty,
                    e.quoteAmount,
                    e.executedAt
                )
                from ExecutionEntity e
                where e.accountId = :accountId
                order by e.executedAt desc, e.executionId desc
                """, ExecutionSummary.class);

        List<ExecutionSummary> content = query
                .setParameter("accountId", accountId.value())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        long totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        boolean hasNext = page + 1 < totalPages;

        return new PageResult<>(
                content,
                page,
                size,
                totalElements,
                (int) totalPages,
                hasNext
        );
    }
}
