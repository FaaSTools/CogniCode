package org.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaudeLLMService extends AWSBedrockLLMService {

    public ClaudeLLMService(String region, String modelIdentifier) {
        super(region, modelIdentifier);
    }

    @Override
    protected String convertMessagesToPrompt(List<ChatMessage> messages) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("anthropic_version", "bedrock-2023-05-31");
            body.put("max_tokens", 5000);
            body.put("top_p", 0.9);
            body.put("temperature", 1.0
            );

            List<Map<String, Object>> messageList =
                messages.stream().map(message -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role",
                                   message.getRole().equalsIgnoreCase("user") ?
                                   "user" : "assistant");

                    Map<String, Object> textContent = new HashMap<>();
                    textContent.put("type", "text");
                    textContent.put("text", message.getContent());

                    messageMap.put("content", List.of(textContent));
                    return messageMap;
                }).toList();

            body.put("messages", messageList);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(body);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected String parseResponse(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode contentNode = rootNode.path("content");
            if (contentNode.isArray() && contentNode.size() > 0) {
                return contentNode.get(0).path("text").asText();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
