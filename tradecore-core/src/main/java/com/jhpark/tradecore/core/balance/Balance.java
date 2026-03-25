package com.jhpark.tradecore.core.balance;

import java.math.BigDecimal;
import java.util.Objects;

// 잔고 생성
public final class Balance {
    private final Asset asset;
    private final BigDecimal available;
    private final BigDecimal locked;

    public Balance(
            Asset asset,
            BigDecimal available,
            BigDecimal locked
    ) {
        this.asset = Objects.requireNonNull(asset, "asset is null");
        this.available = normalize(available, "available");
        this.locked = normalize(locked, "locked");

        if (this.available.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("available은 0 이상이어야 합니다.");
        }

        if (this.locked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("locked는 0 이상이어야 합니다.");
        }
    }

    // 특정 자산의 0 잔고 생성
    public static Balance zero(Asset asset) {
        return new Balance(asset, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // 가용 잔고에서 잠금 잔고로 이동
    public Balance lock(BigDecimal amount) {
        BigDecimal normalizedAmount = positive(amount, "lock amount");

        if (available.compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("available 부족");
        }

        return new Balance(
                asset,
                available.subtract(normalizedAmount),
                locked.add(normalizedAmount)
        );
    }

    // 잠금 잔고에서 가용 잔고로 이동
    public Balance unlock(BigDecimal amount) {
        BigDecimal normalizedAmount = positive(amount, "unlock amount");

        if (locked.compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("locked 부족");
        }

        return new Balance(
                asset,
                available.add(normalizedAmount),
                locked.subtract(normalizedAmount)
        );
    }

    // 총 잔고 반환
    public BigDecimal total() {
        return available.add(locked);
    }

    public Asset getAsset() {
        return asset;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getLocked() {
        return locked;
    }

    // null 여부 검사 및 값 정규화
    private static BigDecimal normalize(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is null");
        }
        return value.stripTrailingZeros();
    }

    // 양수인지 검사
    private static BigDecimal positive(BigDecimal value, String fieldName) {
        BigDecimal normalized = normalize(value, fieldName);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " 은(는) 0보다 커야 합니다.");
        }
        return normalized;
    }
}
