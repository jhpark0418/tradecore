package com.jhpark.tradecore.db.entity.account;

import com.jhpark.tradecore.core.balance.Asset;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "account_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_balances_account_asset",
                        columnNames = {"account_id", "asset"}
                )
        }
)
public class AccountBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset", nullable = false, length = 20)
    private Asset asset;

    @Column(name = "available", nullable = false, precision = 30, scale = 12)
    private BigDecimal available;

    @Column(name = "locked", nullable = false, precision = 30, scale = 12)
    private BigDecimal locked;

    protected AccountBalanceEntity() {}

    public AccountBalanceEntity(
            Asset asset,
            BigDecimal available,
            BigDecimal locked
    ) {
        this.asset = Objects.requireNonNull(asset, "asset is null");
        this.available = Objects.requireNonNull(available, "available is null");
        this.locked = Objects.requireNonNull(locked, "locked is null");
    }

    void attach(AccountEntity account) {
        this.account = Objects.requireNonNull(account, "account is null");
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

    public void updateAmounts(BigDecimal available, BigDecimal locked) {
        this.available = Objects.requireNonNull(available, "available is null");
        this.locked = Objects.requireNonNull(locked, "locked is null");
    }
}
