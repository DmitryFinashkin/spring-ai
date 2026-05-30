package dev.finashkin.springai.services;

import dev.finashkin.springai.model.CodeReview;
import dev.finashkin.springai.utils.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder,
                     ChatMemoryRepository chatMemoryRepository){
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();


        this.chatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
    }

    public String chat(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .call()
                .content();
    }

    public CodeReview codereview(String code) {
        return chatClient.prompt()
                .user("Review this code. Be specific and concise: " + code)
                .call()
                .entity(CodeReview.class);
    }

    public String chatWithTools(String message){

        return chatClient.prompt(message)
                .tools(new WeatherTool())
                .call()
                .content();
    }
}
