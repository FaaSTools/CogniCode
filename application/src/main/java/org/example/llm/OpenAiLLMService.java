package org.example.llm;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.List;

public class OpenAiLLMService implements LLMService {

    private final OpenAiService openAiService;

    public OpenAiLLMService(String apiKey) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(300));
    }

    @Override
    public String sendMessages(List<ChatMessage> messages) {
        ChatCompletionRequest completionRequest =
            ChatCompletionRequest.builder()
                                 .model("gpt-4o")
                                 .messages(messages)
                                 .maxTokens(5000)
                                 .temperature(1.0)
                                 .topP(0.9)
                                 .build();

        return openAiService.createChatCompletion(completionRequest)
                            .getChoices().get(0).getMessage().getContent();
    }
}
