package com.jhpark.tradecore.core.execution;

import java.util.Objects;
import java.util.UUID;

public final class ExecutionId {

    private final String value;

    public ExecutionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("executionId must not be blank");
        }
        this.value = value;
    }

    public static ExecutionId newId() {
        return new ExecutionId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExecutionId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
