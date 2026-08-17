package com.example.bigdecimalkey;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PriceCatalogTest {

    @Test
    void 数値として同じ金額ならスケールが違ってもSKUを検索できる() {
        PriceCatalog catalog = new PriceCatalog();
        BigDecimal registeredPrice = new BigDecimal("10.0");
        BigDecimal requestedPrice = new BigDecimal("10.00");
        catalog.register(registeredPrice, "NOTEBOOK");

        Optional<String> foundSku = catalog.findSku(requestedPrice);

        System.out.printf(
                "[evidence] registered=%s scale=%d requested=%s scale=%d compareTo=%d equals=%s%n",
                registeredPrice,
                registeredPrice.scale(),
                requestedPrice,
                requestedPrice.scale(),
                registeredPrice.compareTo(requestedPrice),
                registeredPrice.equals(requestedPrice));

        assertAll(
                () -> assertEquals(Optional.of("NOTEBOOK"), foundSku,
                        "数値として同じ10.0と10.00なら同じSKUを返すこと"),
                () -> assertEquals(1, catalog.entryCount(),
                        "登録済み価格は1件だけであること"),
                () -> assertEquals(Optional.of(registeredPrice), catalog.registeredPriceFor("NOTEBOOK"),
                        "登録済みの金額は保持されること"),
                () -> assertTrue(registeredPrice.compareTo(requestedPrice) == 0,
                        "数値としては同じ金額であること"));
    }
}
