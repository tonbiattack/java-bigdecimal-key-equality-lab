package com.example.bigdecimalkey;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class PriceCatalog {

    private final Map<BigDecimal, String> skuByPrice = new TreeMap<>();

    public void register(BigDecimal price, String sku) {
        skuByPrice.put(price, sku);
    }

    public Optional<String> findSku(BigDecimal price) {
        return Optional.ofNullable(skuByPrice.get(price));
    }

    public int entryCount() {
        return skuByPrice.size();
    }

    public Optional<BigDecimal> registeredPriceFor(String sku) {
        return skuByPrice.entrySet().stream()
                .filter(entry -> entry.getValue().equals(sku))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
