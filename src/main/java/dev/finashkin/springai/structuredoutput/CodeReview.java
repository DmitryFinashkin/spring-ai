package dev.finashkin.springai.structuredoutput;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Evolved code-review record for the structured-output deep-dive.
 *
 * <p>Layer 1 (schema): the nested {@link Issue} record, the {@link Severity} enum, and the
 * {@code @JsonPropertyDescription} on every field all flow into the JSON schema Spring AI hands
 * the model, so the model aims at the right shape in the first place.
 *
 * <p>Layer 2 (validation): the Jakarta Bean Validation annotations are a runtime guard. They are
 * NOT sent to the model (only the Jackson annotations reach the generated schema) — they reject
 * output that parsed fine but is still wrong (a {@code qualityScore} of 150, a null list).
 */
@JsonPropertyOrder({"summary", "qualityScore", "issues", "suggestions"})
public record CodeReview(

        @NotBlank
        @JsonPropertyDescription("One or two sentence overall summary of the code")
        String summary,

        @Min(0) @Max(100)
        @JsonPropertyDescription("Overall quality from 0 (terrible) to 100 (excellent)")
        int qualityScore,

        @NotNull @Valid
        @JsonPropertyDescription("Specific issues found in the code; empty list if none")
        List<Issue> issues,

        @NotNull @Size(max = 5)
        @JsonPropertyDescription("Up to 5 concrete, actionable improvement suggestions")
        List<String> suggestions
) {
    public record Issue(
            @NotBlank
            @JsonPropertyDescription("Short description of the issue")
            String description,

            @NotNull
            @JsonPropertyDescription("Severity of the issue")
            Severity severity,

            @Min(0)
            @JsonPropertyDescription("Line number where it occurs, or 0 if not line-specific")
            int line
    ) {}

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
}
