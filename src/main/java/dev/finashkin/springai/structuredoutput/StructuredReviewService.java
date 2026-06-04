package dev.finashkin.springai.structuredoutput;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * Production-grade structured output: design the schema (Layer 1, see {@link CodeReview}),
 * validate what comes back (Layer 2), and self-heal when it is wrong (Layer 3).
 *
 * <p>Uses the default (autoconfigured) {@link ChatClient}. For an on-camera demo or production,
 * consider pointing this at a more capable model — inject a qualified cloud client instead of the
 * default builder (see {@code multimodel.CloudChatClientConfig}'s {@code cloudChatClient} bean).
 */
@Service
public class StructuredReviewService {

    private static final int MAX_ATTEMPTS = 3;

    private final ChatClient chatClient;
    private final Validator validator;
    private final BeanOutputConverter<CodeReview> converter =
            new BeanOutputConverter<>(CodeReview.class);

    public StructuredReviewService(ChatClient.Builder builder, Validator validator) {
        this.chatClient = builder.build();
        this.validator = validator;
    }

    /**
     * Layer 1 + 2: parse with {@code .entity()}, then validate. No retry — rejects bad data loudly
     * with {@link InvalidReviewException}. This is the "it parsed, still wrong" demo.
     */
    public CodeReview reviewValidating(String code) {
        CodeReview review = chatClient.prompt()
                .user("Review this Java code:\n" + code)
                .call()
                .entity(CodeReview.class);

        Set<ConstraintViolation<CodeReview>> violations = validator.validate(review);
        if (!violations.isEmpty()) {
            throw new InvalidReviewException(describe(violations));
        }
        return review;
    }

    /**
     * Layer 1 + 2 + 3: parse, validate, and on a parse OR validation failure hand the error and the
     * model's own output back to it for a corrected attempt. Drops to {@link BeanOutputConverter}
     * directly (instead of {@code .entity()}) so we own both the schema instructions
     * ({@code getFormat()}) and the conversion failure.
     */
    public CodeReview review(String code) {
        String format = converter.getFormat();
        String prompt = """
                You are a senior Java code reviewer. Review the code and return ONLY
                JSON matching the schema. No prose before or after the JSON.

                Code:
                %s

                %s
                """.formatted(code, format);

        String lastError = null;
        String lastOutput = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = chatClient.prompt().user(prompt).call().content();
            lastOutput = raw;

            try {
                CodeReview review = converter.convert(raw);           // may throw
                Set<ConstraintViolation<CodeReview>> violations = validator.validate(review);
                if (violations.isEmpty()) {
                    return review;                                    // success
                }
                lastError = describe(violations);                     // business rules
            } catch (Exception parseError) {
                lastError = "JSON parse error: " + parseError.getMessage();
            }

            // hand the error back to the model for the next attempt
            prompt = """
                    Your previous response was invalid.

                    Problem(s): %s

                    Your previous output was:
                    %s

                    Return corrected JSON that fixes these problems and matches the
                    schema exactly. Output ONLY JSON, no prose.

                    %s
                    """.formatted(lastError, lastOutput, format);
        }

        throw new ReviewFailedException(
                "No valid output after %d attempts. Last error: %s"
                        .formatted(MAX_ATTEMPTS, lastError));
    }

    private String describe(Set<ConstraintViolation<CodeReview>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
    }
}
