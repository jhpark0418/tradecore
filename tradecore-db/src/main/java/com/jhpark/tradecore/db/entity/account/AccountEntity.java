package com.jhpark.tradecore.db.entity.account;

import com.jhpark.tradecore.core.balance.Asset;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false, length = 100)
    private String accountId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(
            mappedBy = "account",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AccountBalanceEntity> balances = new ArrayList<>();

    protected AccountEntity() {}

    public AccountEntity(String accountId, Long version) {
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.version = version;
    }

    public String getAccountId() {
        return accountId;
    }

    public Long getVersion() {
        return version;
    }

    public List<AccountBalanceEntity> getBalances() {
        return Collections.unmodifiableList(balances);
    }

    public void addBalance(AccountBalanceEntity balance) {
        balance.attach(this);
        this.balances.add(balance);
    }

    public void removeBalance(AccountBalanceEntity balance) {
        this.balances.remove(balance);
    }

    public Optional<AccountBalanceEntity> findBalanceByAsset(Asset asset) {
        return this.balances.stream()
                .filter(balance -> balance.getAsset() == asset)
                .findFirst();
    }
}
