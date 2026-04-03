package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.ExecutionRepository;
import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.execution.ExecutionId;
import com.jhpark.tradecore.core.market.Symbol;
import com.jhpark.tradecore.core.order.OrderId;
import com.jhpark.tradecore.db.entity.execution.ExecutionEntity;
import com.jhpark.tradecore.db.repository.ExecutionJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ExecutionRepositoryAdapter implements ExecutionRepository {

    private final ExecutionJpaRepository executionJpaRepository;

    public ExecutionRepositoryAdapter(ExecutionJpaRepository executionJpaRepository) {
        this.executionJpaRepository = executionJpaRepository;
    }


    @Override
    public Optional<Execution> findById(ExecutionId executionId) {
        return executionJpaRepository.findById(executionId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Execution save(Execution execution) {
        String executionId = execution.getExecutionId().value();

        if (executionJpaRepository.existsById(executionId)) {
            throw new ConcurrencyConflictException(
                    "Execution already exists. executionId=" + executionId
            );
        }

        try {
            ExecutionEntity saved = executionJpaRepository.saveAndFlush(toEntity(execution));
            return toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConcurrencyConflictException(
                    "Execution already exists. executionId=" + executionId,
                    e
            );
        }
    }

    private ExecutionEntity toEntity(Execution execution) {
        return new ExecutionEntity(
                execution.getExecutionId().value(),
                execution.getOrderId().value(),
                execution.getAccountId().value(),
                execution.getSymbol().baseAsset(),
                execution.getSymbol().quoteAsset(),
                execution.getSide(),
                execution.getExecutionPrice(),
                execution.getExecutionQty(),
                execution.getQuoteAmount(),
                execution.getExecutedAt()
        );
    }

    private Execution toDomain(ExecutionEntity entity) {
        return Execution.create(
                new ExecutionId(entity.getExecutionId()),
                new OrderId(entity.getOrderId()),
                new AccountId(entity.getAccountId()),
                new Symbol(entity.getBaseAsset(), entity.getQuoteAsset()),
                entity.getSide(),
                entity.getExecutionPrice(),
                entity.getExecutionQty(),
                entity.getQuoteAmount(),
                entity.getExecutedAt()
        );
    }
}
