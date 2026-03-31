package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.execution.Execution;
import com.jhpark.tradecore.core.execution.ExecutionId;

import java.util.Optional;

public interface ExecutionRepository {
    Optional<Execution> findById(ExecutionId executionId);
    Execution save(Execution execution);
}
