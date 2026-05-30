package dev.finashkin.springai.multimodel;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CloudChatClientConfig {

    @Value("${app.cloud.api-key}")
    private String cloudApiKey;

    @Value("${app.cloud.model}")
    private String cloudModel;

    @Value("${app.cloud.max-tokens:4096}")
    private Integer cloudMaxTokens;

    @Value("${app.cloud.response-timeout-seconds:300}")
    private Integer cloudResponseTimeoutSeconds;

    @Bean(name = "cloudChatClient")
    public ChatClient cloudChatClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofSeconds(cloudResponseTimeoutSeconds));

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient));

        AnthropicApi api = AnthropicApi.builder()
                .apiKey(cloudApiKey)
                .restClientBuilder(restClientBuilder)
                .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(cloudModel)
                .maxTokens(cloudMaxTokens)
                .build();

        AnthropicChatModel model = AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(model).build();
    }
}
