package org.example.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import java.util.Map;

public class Llama2LLMService extends AWSBedrockLLMService {

    public Llama2LLMService(String region, String modelIdentifier) {
        super(region, modelIdentifier);
    }

    @Override
    protected String convertMessagesToPrompt(List<ChatMessage> messages) {
        try {
            // Build the prompt with roles and message contents
            StringBuilder promptBuilder = new StringBuilder();
            for (ChatMessage message : messages) {
                String role =
                    message.getRole().equalsIgnoreCase("user") ? "User" :
                    "Assistant";
                promptBuilder.append(role).append(": ")
                             .append(message.getContent()).append("\nAssistant: ");
            }
            String prompt = promptBuilder.toString().trim();

            // Create the body structure with prompt and additional parameters
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "prompt", prompt,
                "max_gen_len", 5000,
                "temperature", 0.0,
                "top_p", 0.9
            ));

            return jsonBody;

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

            String response = rootNode.path("generation").asText();
            if (response == null || response.isEmpty()) {
                System.out.println(
                    "Warning: 'generation' field is empty or not found.");
            } else {
                return response;
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println(
                "Error parsing response body JSON: " + e.getMessage());
        }

        return responseBody;
    }
}
