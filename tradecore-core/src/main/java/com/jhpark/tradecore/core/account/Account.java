package com.jhpark.tradecore.core.account;

import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class Account {

    private final AccountId accountId;
    private final Map<Asset, Balance> balances;
    private final long version;

    public Account(
            AccountId accountId,
            Map<Asset, Balance> balances,
            long version
    ) {
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        Objects.requireNonNull(balances, "balances is null");

        this.balances = Collections.unmodifiableMap(copyBalances(balances));

        if (version < 0) {
            throw new IllegalArgumentException("version is negative");
        }
        this.version = version;
    }

    public static Account empty(AccountId accountId) {
        return new Account(accountId, Map.of(), 0L);
    }

    public static Account of(AccountId accountId, Map<Asset, Balance> balances) {
        return new Account(accountId, balances, 0L);
    }

    public long getVersion() {
        return version;
    }

    public Account withVersion(long version) {
        return new Account(this.accountId, this.balances, version);
    }

    public Balance getBalance(Asset asset) {
        Objects.requireNonNull(asset, "asset is null");
        return balances.getOrDefault(asset, Balance.zero(asset));
    }

    public Account withBalance(Balance balance) {
        Objects.requireNonNull(balance, "balance is null");

        EnumMap<Asset, Balance> copied = new EnumMap<>(Asset.class);
        copied.putAll(this.balances);
        copied.put(balance.getAsset(), balance);

        return new Account(accountId, copied, version);
    }

    public Account lock(Asset asset, BigDecimal amount) {
        Balance current = getBalance(asset);
        Balance updated = current.lock(amount);
        return withBalance(updated);
    }

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
        EnumMap<Asset, Balance> copied = new EnumMap<>(Asset.class);

        for (Map.Entry<Asset, Balance> entry : source.entrySet()) {
            Asset asset = Objects.requireNonNull(entry.getKey(), "asset is null");
            Balance balance = Objects.requireNonNull(entry.getValue(), "balance is null");

            if (balance.getAsset() != asset) {
                throw new IllegalArgumentException("Balance의 asset과 Map key asset이 다릅니다.");
            }

            copied.put(asset, balance);
        }

        return copied;
    }
}