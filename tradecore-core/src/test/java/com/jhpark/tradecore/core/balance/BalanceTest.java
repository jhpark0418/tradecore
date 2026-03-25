package com.jhpark.tradecore.core.balance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BalanceTest {

    @Test
    void zeroBalanceCanBeCreated() {
        Balance balance = Balance.zero(Asset.USDT);

        assertEquals(0, balance.getAvailable().compareTo(BigDecimal.ZERO));
        assertEquals(0, balance.getLocked().compareTo(BigDecimal.ZERO));
        assertEquals(0, balance.total().compareTo(BigDecimal.ZERO));
    }

    @Test
    void lockMovesAmountFromAvailableToLocked() {
        Balance balance = new Balance(
                Asset.USDT,
                new BigDecimal("1000"),
                new BigDecimal("0")
        );

        Balance lockedBalance = balance.lock(new BigDecimal("250"));

        assertEquals(0, lockedBalance.getAvailable().compareTo(new BigDecimal("750")));
        assertEquals(0, lockedBalance.getLocked().compareTo(new BigDecimal("250")));
        assertEquals(0, lockedBalance.total().compareTo(new BigDecimal("1000")));
    }

    @Test
    void lockFailsWhenAvailableIsInsufficient() {
        Balance balance = new Balance(
                Asset.USDT,
                new BigDecimal("100"),
                new BigDecimal("0")
        );

        assertThrows(IllegalArgumentException.class, () ->
                balance.lock(new BigDecimal("150"))
        );
    }

    @Test
    void unlockMovesAmountFromLockedToAvailable() {
        Balance balance = new Balance(
                Asset.USDT,
                new BigDecimal("700"),
                new BigDecimal("300")
        );

        Balance unlockedBalance = balance.unlock(new BigDecimal("100"));

        assertEquals(0, unlockedBalance.getAvailable().compareTo(new BigDecimal("800")));
        assertEquals(0, unlockedBalance.getLocked().compareTo(new BigDecimal("200")));
        assertEquals(0, unlockedBalance.total().compareTo(new BigDecimal("1000")));
    }

    @Test
    void unlockFailsWhenLockedIsInsufficient() {
        Balance balance = new Balance(
                Asset.USDT,
                new BigDecimal("700"),
                new BigDecimal("300")
        );

        assertThrows(IllegalArgumentException.class, () ->
                balance.unlock(new BigDecimal("400"))
        );
    }
}