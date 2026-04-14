package com.jhpark.tradecore.core.market;

import com.jhpark.tradecore.core.balance.Asset;

import java.util.Locale;

public final class SymbolParser {

    private SymbolParser() {}

    public static Symbol parse(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }

        String normalized = symbol.trim().toUpperCase(Locale.ROOT);

        for (Asset baseAsset : Asset.values()) {
            for (Asset quoteAsset : Asset.values()) {
                if (baseAsset == quoteAsset) {
                    continue;
                }

                if ((baseAsset.name() + quoteAsset.name()).equals(normalized)) {
                    return new Symbol(baseAsset, quoteAsset);
                }
            }
        }

        throw new IllegalArgumentException("지원하지 않는 symbol 입니다. symbol=" + symbol);
    }
}
