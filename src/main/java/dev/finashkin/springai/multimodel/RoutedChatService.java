package dev.finashkin.springai.multimodel;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RoutedChatService {

    private final ChatClient localClient;
    private final ChatClient cloudClient;
    private final QueryRouter router;
    private final CostTracker tracker;

    public RoutedChatService(
            ChatClient.Builder localBuilder,
            @Qualifier("cloudChatClient") ChatClient cloudClient,
            QueryRouter router,
            CostTracker tracker) {
        this.localClient = localBuilder.build();
        this.cloudClient = cloudClient;
        this.router = router;
        this.tracker = tracker;
    }

    public RoutedResponse route(String prompt) {
        RoutingDecision decision = router.route(prompt);
        ChatClient client = (decision.tier() == ModelTier.LOCAL) ? localClient : cloudClient;

        ChatResponse response = client.prompt(prompt).call().chatResponse();
        String text = response.getResult().getOutput().getText();

        long[] tokens = extractTokens(response, prompt, text);
        tracker.record(decision, tokens[0], tokens[1]);

        return new RoutedResponse(decision, text);
    }

    private long[] extractTokens(ChatResponse response, String prompt, String responseText) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();

        Long input = usage == null ? null : asLong(usage.getPromptTokens());
        Long output = usage == null ? null : asLong(usage.getCompletionTokens());

        if (input == null || input == 0L) {
            input = estimateTokens(prompt);
        }
        if (output == null || output == 0L) {
            output = estimateTokens(responseText);
        }

        return new long[] { input, output };
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private long estimateTokens(String text) {
        return text == null ? 0L : Math.max(1L, text.length() / 4L);
    }
}
