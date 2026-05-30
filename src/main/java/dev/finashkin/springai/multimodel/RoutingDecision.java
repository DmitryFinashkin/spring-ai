package dev.finashkin.springai.multimodel;

import java.math.BigDecimal;
import java.time.Instant;

public record RoutingDecision(
        ModelTier tier,
        String rule,
        String excerpt,
        Instant timestamp,
        long inputTokens,
        long outputTokens,
        BigDecimal costUsd
) {
    public RoutingDecision(ModelTier tier, String rule, String excerpt, Instant timestamp) {
        this(tier, rule, excerpt, timestamp, 0L, 0L, BigDecimal.ZERO);
    }

    public RoutingDecision withUsage(long inputTokens, long outputTokens, BigDecimal costUsd) {
        return new RoutingDecision(tier, rule, excerpt, timestamp, inputTokens, outputTokens, costUsd);
    }
}
