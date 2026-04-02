package com.jhpark.tradecore.core.support.fake;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
public class FakeAccountRepository implements AccountRepository {

    private final Map<String, Account> storage = new HashMap<>();
    private boolean simulateConcurrentUpdateOnNextSave;

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return Optional.ofNullable(storage.get(accountId.value()));
    }

    @Override
    public Account save(Account account) {
        Account current = storage.get(account.getAccountId().value());

        if (current == null) {
            if (account.getVersion() != 0L) {
                throw new ConcurrencyConflictException(
                        "New account must start with version 0. accountId=" + account.getAccountId().value()
                );
            }

            Account persisted = account.withVersion(1L);
            storage.put(account.getAccountId().value(), persisted);
            return persisted;
        }

        if (simulateConcurrentUpdateOnNextSave) {
            simulateConcurrentUpdateOnNextSave = false;
            Account bumped = current.withVersion(current.getVersion() + 1);
            storage.put(account.getAccountId().value(), bumped);
            current = bumped;
        }

        if (account.getVersion() != current.getVersion()) {
            throw new ConcurrencyConflictException(
                    "Account version conflict occurred for accountId=" + account.getAccountId().value()
                            + ", expected=" + current.getVersion()
                            + ", actual=" + account.getVersion()
            );
        }

        Account persisted = account.withVersion(current.getVersion() + 1);
        storage.put(account.getAccountId().value(), persisted);
        return persisted;
    }

    public void simulateConcurrentUpdateOnNextSave() {
        this.simulateConcurrentUpdateOnNextSave = true;
    }

    public void clear() {
        storage.clear();
    }

    public int size() {
        return storage.size();
    }
}
