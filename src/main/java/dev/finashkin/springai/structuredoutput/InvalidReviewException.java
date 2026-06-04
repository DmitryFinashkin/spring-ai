package dev.finashkin.springai.structuredoutput;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by the validate-only path (Layer 2) when the model output parsed cleanly but violated a
 * business rule. Surfaces as 422 instead of a Jackson 500 — fail loud, not silent.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidReviewException extends RuntimeException {

    public InvalidReviewException(String message) {
        super(message);
    }
}
