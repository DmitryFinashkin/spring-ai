package dev.finashkin.springai.multimodel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CostSnapshot(
        Map<ModelTier, TierStats> tiers,
        BigDecimal totalCostUsd,
        List<RoutingDecision> recent
) {
    public record TierStats(
            long requests,
            long inputTokens,
            long outputTokens,
            BigDecimal costUsd
    ) {
    }
}
