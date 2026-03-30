package com.jhpark.tradecore.core.account;

import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class Account {

    private final AccountId accountId;
    private final Map<Asset, Balance> balances;

    public Account(
            AccountId accountId,
            Map<Asset, Balance> balances
    ) {
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.balances = Collections.unmodifiableMap(copyBalances(balances));
    }

    // 잔고 없는 빈 계정 생성
    public static Account empty(AccountId accountId) {
        return new Account(accountId, Map.of());
    }

    // 특정 자산의 잔고 반환, 없으면 0 잔고 반환
    public Balance getBalance(Asset asset) {
        Objects.requireNonNull(asset, "asset is null");
        return balances.getOrDefault(asset, Balance.zero(asset));
    }

    // 특정 자산 잔고를 교체한 새 Account 반환
    public Account withBalance(Balance balance) {
        Objects.requireNonNull(balance, "balance is null");

        EnumMap<Asset, Balance> copied = new EnumMap<>(Asset.class);
        copied.putAll(this.balances);
        copied.put(balance.getAsset(), balance);

        return new Account(accountId, copied);
    }

    // 특정 자산의 가용 잔고를 잠금 잔고로 이동
    public Account lock(Asset asset, BigDecimal amount) {
        Balance current = getBalance(asset);
        Balance updated = current.lock(amount);
        return withBalance(updated);
    }

    // 특정 자산의 잠금 잔고를 가용 잔고로 이동
    public Account unlock(Asset asset, BigDecimal amount) {
        Balance current = getBalance(asset);
        Balance updated = current.unlock(amount);
        return withBalance(updated);
    }

    public Account decreaseLocked(Asset asset, BigDecimal amount) {
        Balance current = getBalance(asset);
        Balance updated = current.decreaseLocked(amount);
        return withBalance(updated);
    }

    public Account increaseAvailable(Asset asset, BigDecimal amount) {
        Balance current = getBalance(asset);
        Balance updated = current.increaseAvailable(amount);
        return withBalance(updated);
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public Map<Asset, Balance> getBalances() {
        return balances;
    }

    private static EnumMap<Asset, Balance> copyBalances(Map<Asset, Balance> source) {
        Objects.requireNonNull(source, "source is null");

        EnumMap<Asset, Balance> copied = new EnumMap<>(Asset.class);

        for (Map.Entry<Asset, Balance> entry : source.entrySet()) {
            Asset asset = Objects.requireNonNull(entry.getKey(), "asset is null");
            Balance balance = Objects.requireNonNull(entry.getValue(), "balance is null");

            if (balance.getAsset() != asset) {
                throw new IllegalArgumentException("asset 코드가 다릅니다.");
            }

            copied.put(asset, balance);
        }

        return copied;
    }
}
