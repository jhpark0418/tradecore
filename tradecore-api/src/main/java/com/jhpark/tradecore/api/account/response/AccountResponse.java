package com.jhpark.tradecore.api.account.response;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public record AccountResponse(
        String accountId,
        long version,
        Map<String, BalanceSnapshot> balances
) {
    public static AccountResponse from(Account account) {
        Map<String, BalanceSnapshot> balanceMap = new TreeMap<>();

        for (Asset asset : Asset.values()) {
            Balance balance = account.getBalance(asset);

            balanceMap.put(
                    asset.name(),
                    new BalanceSnapshot(
                            balance.getAvailable(),
                            balance.getLocked(),
                            balance.total()
                    )
            );
        }

        return new AccountResponse(
                account.getAccountId().value(),
                account.getVersion(),
                balanceMap
        );
    }

    public record BalanceSnapshot(
            BigDecimal available,
            BigDecimal locked,
            BigDecimal total
    ) {
    }
}
