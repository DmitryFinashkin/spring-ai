package dev.finashkin.springai.structuredoutput;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Surfaces the failure reason for the structured-output endpoints as a small JSON body, so the
 * validation/parse message (e.g. "qualityScore must be less than or equal to 100") is visible
 * instead of being swallowed by the default error page. Scoped to {@link StructuredReviewController}
 * so the other controllers keep their existing behavior.
 */
@RestControllerAdvice(assignableTypes = StructuredReviewController.class)
public class StructuredReviewExceptionHandler {

    public record ApiError(int status, String error, String message) {}

    @ExceptionHandler(InvalidReviewException.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidReviewException ex) {
        return build(ex.getMessage());
    }

    @ExceptionHandler(ReviewFailedException.class)
    public ResponseEntity<ApiError> handleFailed(ReviewFailedException ex) {
        return build(ex.getMessage());
    }

    private ResponseEntity<ApiError> build(String message) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message));
    }
}
