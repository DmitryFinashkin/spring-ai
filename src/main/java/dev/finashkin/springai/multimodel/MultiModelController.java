package dev.finashkin.springai.multimodel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class MultiModelController {

    private final RoutedChatService routedChatService;
    private final CostTracker costTracker;

    public MultiModelController(RoutedChatService routedChatService, CostTracker costTracker) {
        this.routedChatService = routedChatService;
        this.costTracker = costTracker;
    }

    @PostMapping("/route")
    public RoutedResponse route(@RequestBody String prompt) {
        return routedChatService.route(prompt);
    }

    @GetMapping("/costs")
    public CostSnapshot costs() {
        return costTracker.snapshot();
    }

    @PostMapping("/costs/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        costTracker.reset();
    }
}
