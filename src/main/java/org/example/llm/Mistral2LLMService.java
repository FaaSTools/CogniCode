package org.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mistral2LLMService extends AWSBedrockLLMService {

    public Mistral2LLMService(String region, String modelIdentifier) {
        super(region, modelIdentifier);
    }

    @Override
    protected String convertMessagesToPrompt(List<ChatMessage> messages) {
        try {
            // Build the messages list
            List<Map<String, String>> messageList =
                messages.stream().map(message -> {
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("role",
                                   message.getRole().equalsIgnoreCase("user") ?
                                   "user" : "assistant");
                    messageMap.put("content", message.getContent());
                    return messageMap;
                }).toList();

            // Create the body with parameters and messages
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("max_tokens", 5000);
            bodyMap.put("top_p", 0.9);
            bodyMap.put("stop", List.of());
            bodyMap.put("temperature", 1.0);
            bodyMap.put("messages", messageList);

            // Convert the body to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(bodyMap);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected String parseResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Navigate to "choices" array, then access the "message" object and its "content" field
            JsonNode contentNode =
                rootNode.path("choices").get(0).path("message").path("content");

            return contentNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
