package com.jhpark.tradecore.api.account;

import com.jhpark.tradecore.api.account.response.AccountResponse;
import com.jhpark.tradecore.application.TradingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountQueryController {

    private final TradingQueryService tradingQueryService;

    public AccountQueryController(TradingQueryService tradingQueryService) {
        this.tradingQueryService = tradingQueryService;
    }

    @GetMapping("/api/accounts/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return AccountResponse.from(tradingQueryService.getAccount(accountId));
    }
}
