package com.jhpark.tradecore.bootstrap;

import com.jhpark.tradecore.core.account.Account;
import com.jhpark.tradecore.core.account.AccountId;
import com.jhpark.tradecore.core.application.port.out.AccountRepository;
import com.jhpark.tradecore.core.balance.Asset;
import com.jhpark.tradecore.core.balance.Balance;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner initDemoAccount(AccountRepository accountRepository) {
        return args -> {
            AccountId accountId = new AccountId("demo-user-1");

            if (accountRepository.findById(accountId).isPresent()) {
                return;
            }

            Account demoAccount = Account.of(
                    accountId,
                    Map.of(
                            Asset.USDT, new Balance(Asset.USDT, new BigDecimal("100000"), BigDecimal.ZERO),
                            Asset.BTC, new Balance(Asset.BTC, new BigDecimal("1.5"), BigDecimal.ZERO),
                            Asset.ETH, new Balance(Asset.ETH, new BigDecimal("10"), BigDecimal.ZERO)
                    )
            );

            accountRepository.save(demoAccount);
        };
    }
}
