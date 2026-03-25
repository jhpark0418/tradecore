package com.jhpark.tradecore.core.account;

public record AccountId(String value) {
    public AccountId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("계정 ID는 비어 있을 수 없습니다.");
        }
    }
}
