package dev.finashkin.springai.multimodel;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.routing.cost")
public record ModelPricing(TierPricing local, TierPricing cloud) {

    public record TierPricing(
            BigDecimal inputPerMillionUsd,
            BigDecimal outputPerMillionUsd
    ) {
    }

    public BigDecimal costFor(ModelTier tier, long inputTokens, long outputTokens) {
        TierPricing p = (tier == ModelTier.LOCAL) ? local : cloud;
        BigDecimal million = BigDecimal.valueOf(1_000_000);
        return p.inputPerMillionUsd().multiply(BigDecimal.valueOf(inputTokens))
                .add(p.outputPerMillionUsd().multiply(BigDecimal.valueOf(outputTokens)))
                .divide(million, 6, RoundingMode.HALF_UP);
    }
}
