package dev.finashkin.springai.multimodel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class CostTracker {

    private static final int RECENT_DECISIONS_CAPACITY = 20;

    private final Map<ModelTier, AtomicLong> requests = new EnumMap<>(ModelTier.class);
    private final Map<ModelTier, AtomicLong> inputTokens = new EnumMap<>(ModelTier.class);
    private final Map<ModelTier, AtomicLong> outputTokens = new EnumMap<>(ModelTier.class);
    private final Deque<RoutingDecision> recent = new LinkedList<>();
    private final ModelPricing pricing;

    public CostTracker(ModelPricing pricing) {
        this.pricing = pricing;
        for (ModelTier tier : ModelTier.values()) {
            requests.put(tier, new AtomicLong());
            inputTokens.put(tier, new AtomicLong());
            outputTokens.put(tier, new AtomicLong());
        }
    }

    public void record(RoutingDecision decision, long inTokens, long outTokens) {
        ModelTier tier = decision.tier();
        requests.get(tier).incrementAndGet();
        inputTokens.get(tier).addAndGet(inTokens);
        outputTokens.get(tier).addAndGet(outTokens);

        BigDecimal cost = pricing.costFor(tier, inTokens, outTokens);
        RoutingDecision withUsage = decision.withUsage(inTokens, outTokens, cost);
        synchronized (recent) {
            recent.addFirst(withUsage);
            while (recent.size() > RECENT_DECISIONS_CAPACITY) {
                recent.removeLast();
            }
        }
    }

    public CostSnapshot snapshot() {
        Map<ModelTier, CostSnapshot.TierStats> tiers = new EnumMap<>(ModelTier.class);
        BigDecimal total = BigDecimal.ZERO;

        for (ModelTier tier : ModelTier.values()) {
            long req = requests.get(tier).get();
            long in = inputTokens.get(tier).get();
            long out = outputTokens.get(tier).get();
            BigDecimal cost = pricing.costFor(tier, in, out);
            tiers.put(tier, new CostSnapshot.TierStats(req, in, out, cost));
            total = total.add(cost);
        }

        List<RoutingDecision> recentCopy;
        synchronized (recent) {
            recentCopy = new ArrayList<>(recent);
        }

        return new CostSnapshot(tiers, total, recentCopy);
    }

    public void reset() {
        for (ModelTier tier : ModelTier.values()) {
            requests.get(tier).set(0);
            inputTokens.get(tier).set(0);
            outputTokens.get(tier).set(0);
        }
        synchronized (recent) {
            recent.clear();
        }
    }
}
