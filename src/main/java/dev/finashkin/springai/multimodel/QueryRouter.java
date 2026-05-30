package dev.finashkin.springai.multimodel;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class QueryRouter {

    private static final int LOCAL_MAX_CHARS = 500;

    private static final List<String> COMPLEX_KEYWORDS = List.of(
            "architecture",
            "design",
            "refactor",
            "security",
            "performance",
            "scalability",
            "tradeoff",
            "compare",
            "analyze",
            "best practice"
    );

    public RoutingDecision route(String prompt) {
        String safe = prompt == null ? "" : prompt;
        String excerpt = excerptOf(safe);
        Instant now = Instant.now();

        if (safe.length() > LOCAL_MAX_CHARS) {
            return new RoutingDecision(ModelTier.CLOUD, "LENGTH>" + LOCAL_MAX_CHARS, excerpt, now);
        }

        String lower = safe.toLowerCase();
        for (String keyword : COMPLEX_KEYWORDS) {
            if (lower.contains(keyword)) {
                return new RoutingDecision(ModelTier.CLOUD, "KEYWORD:" + keyword, excerpt, now);
            }
        }

        return new RoutingDecision(ModelTier.LOCAL, "DEFAULT", excerpt, now);
    }

    private String excerptOf(String prompt) {
        String collapsed = prompt.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= 80 ? collapsed : collapsed.substring(0, 77) + "...";
    }
}
