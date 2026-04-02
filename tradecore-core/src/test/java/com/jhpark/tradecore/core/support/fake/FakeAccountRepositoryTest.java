package com.jhpark.tradecore.core.support.fake;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FakeAccountRepositoryTest {

    @Test
    void saveNewAccountShouldIncreaseVersionToOne() {
        FakeAccountRepository repository = new FakeAccountRepository();

        AccountId accountId = new AccountId("account-1");
        Account newAccount = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

        Account saved = repository.save(newAccount);

        assertEquals(1L, saved.getVersion());
        assertEquals(1, repository.size());
    }

    @Test
    void sameAccountVersionCannotBeSavedTwice() {
        FakeAccountRepository repository = new FakeAccountRepository();

        AccountId accountId = new AccountId("account-1");

        Account newAccount = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

        Account persisted = repository.save(newAccount);

        Account snapshot1 = persisted;
        Account snapshot2 = persisted;

        Account updated1 = snapshot1.lock(Asset.USDT, new BigDecimal("100"));
        Account saved1 = repository.save(updated1);

        assertEquals(2L, saved1.getVersion());
        assertEquals(0, saved1.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("900")));
        assertEquals(0, saved1.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));

        Account updated2 = snapshot2.lock(Asset.USDT, new BigDecimal("50"));

        assertThrows(ConcurrencyConflictException.class, () -> repository.save(updated2));
    }

    @Test
    void saveExistingAccountWithLatestVersionShouldIncreaseVersion() {
        FakeAccountRepository repository = new FakeAccountRepository();

        AccountId accountId = new AccountId("account-1");

        Account newAccount = Account.empty(accountId)
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO));

        Account persisted = repository.save(newAccount);
        Account updated = persisted.lock(Asset.USDT, new BigDecimal("200"));

        Account saved = repository.save(updated);

        assertEquals(2L, saved.getVersion());
        assertEquals(0, saved.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, saved.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
    }
}
