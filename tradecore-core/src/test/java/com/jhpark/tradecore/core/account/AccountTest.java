package com.jhpark.tradecore.core.account;

import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AccountTest {

    @Test
    void emptyAccountReturnsZeroBalanceForMissingAsset() {
        Account account = Account.empty(new AccountId("account-1"));

        Balance usdtBalance = account.getBalance(Asset.USDT);

        assertEquals(0, usdtBalance.getAvailable().compareTo(BigDecimal.ZERO));
        assertEquals(0, usdtBalance.getLocked().compareTo(BigDecimal.ZERO));
    }

    @Test
    void lockUpdatesOnlyTargetAssetBalance() {
        Account account = Account.empty(new AccountId("account-1"))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("1000"), BigDecimal.ZERO))
                .withBalance(new Balance(Asset.BTC, new BigDecimal("2"), BigDecimal.ZERO));

        Account lockedAccount = account.lock(Asset.USDT, new BigDecimal("300"));

        assertEquals(0, lockedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("700")));
        assertEquals(0, lockedAccount.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("300")));

        assertEquals(0, lockedAccount.getBalance(Asset.BTC).getAvailable().compareTo(new BigDecimal("2")));
        assertEquals(0, lockedAccount.getBalance(Asset.BTC).getLocked().compareTo(BigDecimal.ZERO));
    }

    @Test
    void unlockRestoresAvailableBalance() {
        Account account = Account.empty(new AccountId("account-1"))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("700"), new BigDecimal("300")));

        Account unlockedAccount = account.unlock(Asset.USDT, new BigDecimal("100"));

        assertEquals(0, unlockedAccount.getBalance(Asset.USDT).getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, unlockedAccount.getBalance(Asset.USDT).getLocked().compareTo(new BigDecimal("200")));
    }

    @Test
    void lockFailsWhenBalanceIsInsufficient() {
        Account account = Account.empty(new AccountId("account-1"))
                .withBalance(new Balance(Asset.USDT, new BigDecimal("100"), BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                account.lock(Asset.USDT, new BigDecimal("200"))
        );
    }
}