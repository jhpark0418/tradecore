package com.jhpark.tradecore.db.repository;

import com.jhpark.tradecore.db.entity.execution.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionJpaRepository extends JpaRepository<ExecutionEntity, String> {

}
