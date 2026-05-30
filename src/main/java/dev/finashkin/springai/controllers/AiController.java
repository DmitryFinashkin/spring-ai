package dev.finashkin.springai.controllers;

import dev.finashkin.springai.services.AiService;
import dev.finashkin.springai.model.CodeReview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message, @RequestParam(defaultValue = "default") String conversationId) {

        return aiService.chat(message, conversationId);
    }

    @PostMapping("/review")
    public CodeReview review(@RequestBody String code) {
        return aiService.codereview(code);
    }

    @GetMapping("/chat-with-tools")
    public String chatWithTools(@RequestParam String message) {
        return aiService.chatWithTools(message);
    }
}
