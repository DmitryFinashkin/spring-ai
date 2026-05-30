package dev.finashkin.springai.multimodel;

public record RoutedResponse(
        RoutingDecision decision,
        String response
) {
}
