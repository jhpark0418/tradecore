package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.exception.ConcurrencyConflictException;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountRepositoryAdapter.class)
@ContextConfiguration(classes = DbTestApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private AccountRepositoryAdapter accountRepositoryAdapter;

    @Test
    void saveAndFindById() {
        Account account = Account.empty(new AccountId("account-1"))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), new BigDecimal("200")))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("1.5"), BigDecimal.ZERO));

        Account saved = accountRepositoryAdapter.save(account);
        Optional<Account> found = accountRepositoryAdapter.findById(new AccountId("account-1"));

        assertEquals(0L, saved.getVersion());
        assertTrue(found.isPresent());
        assertEquals("account-1", found.get().getAccountId().value());
        assertEquals(0, found.get().getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("1000")));
        assertEquals(0, found.get().getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
        assertEquals(0, found.get().getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("1.5")));
        assertEquals(0, found.get().getBalance(Asset.BTC).getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(0L, found.get().getVersion());
    }

    @Test
    void staleVersionThrowsConcurrencyConflict() {
        Account initial = accountRepositoryAdapter.save(
                Account.empty(new AccountId("account-2"))
                        .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                        .withBalance(new Balance(Asset.BTC, new BigDecimal("2"), BigDecimal.ZERO))
        );

        Account updated = initial.lock(Asset.USDT, new BigDecimal("100"));
        Account persistedUpdated = accountRepositoryAdapter.save(updated);

        Account reloaded = accountRepositoryAdapter.findById(new AccountId("account-2"))
                .orElseThrow();

        assertEquals(1L, reloaded.getVersion());
        assertEquals(0, persistedUpdated.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("900")));
        assertEquals(0, persistedUpdated.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("100")));

        Account stale = initial.increaseAvailable(Asset.BTC, new BigDecimal("1"));

        ConcurrencyConflictException exception = assertThrows(
                ConcurrencyConflictException.class,
                () -> accountRepositoryAdapter.save(stale)
        );

        assertTrue(exception.getMessage().contains("account-2"));
    }
}