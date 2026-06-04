package dev.finashkin.springai.structuredoutput;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the structured-output deep-dive. Kept on a dedicated {@code /structured} base path
 * so the original {@code POST /review} demo (Model Switching) stays intact.
 */
@RestController
@RequestMapping("/structured")
public class StructuredReviewController {

    private final StructuredReviewService service;

    public StructuredReviewController(StructuredReviewService service) {
        this.service = service;
    }

    /** Layer 1 + 2 only: validates and rejects bad data with 422, no retry. */
    @PostMapping("/review/strict")
    public CodeReview reviewStrict(@RequestBody String code) {
        return service.reviewValidating(code);
    }

    /** Full self-healing pipeline (Layer 1 + 2 + 3). */
    @PostMapping("/review")
    public CodeReview review(@RequestBody String code) {
        return service.review(code);
    }
}
