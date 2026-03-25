package com.jhpark.tradecore.core.market;

import com.jhpark.tradecore.core.balance.Asset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolTest {

    @Test
    void symbolValueReturnsConcatenatedCode() {
        Symbol symbol = new Symbol(Asset.BTC, Asset.USDT);

        assertEquals("BTCUSDT", symbol.value());
    }

    @Test
    void sameBaseAndQuoteAssetAreNotAllowed() {
        assertThrows(IllegalArgumentException.class, () ->
                new Symbol(Asset.BTC, Asset.BTC)
        );
    }

}
