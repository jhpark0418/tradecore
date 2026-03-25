package com.jhpark.tradecore.core.market;

import com.jhpark.tradecore.core.balance.Asset;

import java.util.Objects;

public record Symbol(
        Asset baseAsset,
        Asset quoteAsset
) {
    public Symbol {
        Objects.requireNonNull(baseAsset, "baseAsset is null");
        Objects.requireNonNull(quoteAsset, "quoteAsset is null");

        if (baseAsset == quoteAsset) {
            throw new IllegalArgumentException("기준 자산과 상대 자산은 같을 수 없습니다.");
        }
    }

    public String value() {
        return baseAsset.name() + quoteAsset.name();
    }

    public boolean isBaseAsset(Asset asset) {
        return baseAsset == asset;
    }

    public boolean isQuoteAsset(Asset asset) {
        return quoteAsset == asset;
    }
}
