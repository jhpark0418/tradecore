package com.jhpark.tradecore.db.repository;

import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import com.jhpark.tradecore.db.entity.outbox.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
