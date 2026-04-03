package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.db.entity.account.AccountBalanceEntity;
import com.jhpark.tradecore.db.entity.account.AccountEntity;
import com.jhpark.tradecore.db.repository.AccountJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(readOnly=true)
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final EntityManager entityManager;

    public AccountRepositoryAdapter(
            AccountJpaRepository accountJpaRepository,
            EntityManager entityManager
    ) {
        this.accountJpaRepository = accountJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return accountJpaRepository.findWithBalancesByAccountId(accountId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Account save(Account account) {
        AccountEntity current = accountJpaRepository.findWithBalancesByAccountId(account.getAccountId().value())
                .orElse(null);

        if (current == null) {
            if (account.getVersion() != 0L) {
                throw new ConcurrencyConflictException(
                        "New account must start with version 0. accountId=" + account.getAccountId().value()
                );
            }

            AccountEntity created = new AccountEntity(account.getAccountId().value(), null);

            for (Map.Entry<Asset, Balance> entry : account.getBalances().entrySet()) {
                Balance balance = entry.getValue();
                created.addBalance(new AccountBalanceEntity(
                        balance.getAsset(),
                        balance.getAvailable(),
                        balance.getLocked()
                ));
            }

            AccountEntity saved = accountJpaRepository.saveAndFlush(created);
            return toDomain(saved);
        }

        long currentVersion = current.getVersion() == null ? 0L : current.getVersion();
        if (account.getVersion() != currentVersion) {
            throw new ConcurrencyConflictException(
                    "Account version conflict occurred for accountId=" + account.getAccountId().value()
                            + ", expected=" + currentVersion
                            + ", actual=" + account.getVersion()
            );
        }

        try {
            syncBalances(current, account);

            entityManager.lock(current, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            entityManager.flush();
            entityManager.refresh(current);

            return toDomain(current);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new ConcurrencyConflictException(
                    "Account optimistic lock conflict occurred for accountId=" + account.getAccountId().value(),
                    e
            );
        }
    }

    private void syncBalances(AccountEntity entity, Account account) {
        Set<Asset> desiredAssets = account.getBalances().keySet();

        List<AccountBalanceEntity> toRemove = new ArrayList<>();
        for (AccountBalanceEntity existingBalance : entity.getBalances()) {
            if (!desiredAssets.contains(existingBalance.getAsset())) {
                toRemove.add(existingBalance);
            }
        }

        for (AccountBalanceEntity balanceToRemove : toRemove) {
            entity.removeBalance(balanceToRemove);
        }

        for (Map.Entry<Asset, Balance> entry : account.getBalances().entrySet()) {
            Asset asset = entry.getKey();
            Balance desired = entry.getValue();

            entity.findBalanceByAsset(asset)
                    .ifPresentOrElse(
                            existing -> existing.updateAmounts(
                                    desired.getAvailable(),
                                    desired.getLocked()
                            ),
                            () -> entity.addBalance(new AccountBalanceEntity(
                                    desired.getAsset(),
                                    desired.getAvailable(),
                                    desired.getLocked()
                            ))
                    );
        }
    }

    private Account toDomain(AccountEntity entity) {
        EnumMap<Asset, Balance> balances = new EnumMap<>(Asset.class);

        for (AccountBalanceEntity balanceEntity : entity.getBalances()) {
            Balance balance = new Balance(
                    balanceEntity.getAsset(),
                    balanceEntity.getAvailable(),
                    balanceEntity.getLocked()
            );
            balances.put(balance.getAsset(), balance);
        }

        long version = entity.getVersion() == null ? 0L : entity.getVersion();

        return new Account(
                new AccountId(entity.getAccountId()),
                balances,
                version
        );
    }
}
