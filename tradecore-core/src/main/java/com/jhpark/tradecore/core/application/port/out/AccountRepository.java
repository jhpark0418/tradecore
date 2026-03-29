package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId accountId);
    Account save(Account account);
}
