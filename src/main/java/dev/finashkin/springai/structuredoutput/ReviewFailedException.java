package dev.finashkin.springai.structuredoutput;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by the self-healing pipeline (Layer 3) after every attempt has been exhausted. Reaching
 * this is the correct loud failure when the model cannot satisfy the schema + rules in time.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ReviewFailedException extends RuntimeException {

    public ReviewFailedException(String message) {
        super(message);
    }
}
