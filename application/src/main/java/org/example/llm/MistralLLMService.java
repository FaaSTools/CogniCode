package org.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import java.util.Map;

public class MistralLLMService extends AWSBedrockLLMService {

    public MistralLLMService(String region, String modelIdentifier) {
        super(region, modelIdentifier);
    }

    @Override
    protected String convertMessagesToPrompt(List<ChatMessage> messages) {
        try {
            // Build the prompt with roles and message contents
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("<s>[INST] ");
            for (ChatMessage message : messages) {
                String role =
                    message.getRole().equalsIgnoreCase("user") ? "User" :
                    "Assistant";
                promptBuilder.append(role).append(": ")
                             .append(message.getContent()).append(" ");
            }
            promptBuilder.append("[/INST]");

            String prompt = promptBuilder.toString().trim();

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "prompt", prompt,
                "max_tokens", 5000,
                "temperature", 0.5,
                "top_p", 0.9,
                "top_k", 50
            ));

            return jsonBody;

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
            JsonNode textNode = rootNode.path("outputs").get(0).path("text");

            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
