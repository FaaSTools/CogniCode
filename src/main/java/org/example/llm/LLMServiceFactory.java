package org.example.llm;

import software.amazon.awssdk.regions.Region;

public class LLMServiceFactory {

    private LLMServiceFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static LLMService getLLMService(String serviceType, String apiKey) {

        String normalizedType = serviceType.toLowerCase();
        return switch (normalizedType) {
            case "openai" -> new OpenAiLLMService(apiKey);
            case "claude" -> new ClaudeLLMService(Region.US_EAST_1.toString(),
                                                  "us.anthropic.claude-3-5-sonnet-20241022-v2:0");
            case "llama" -> new LlamaLLMService(Region.US_EAST_1.toString(),
                                                "us.meta.llama3-1-70b-instruct-v1:0");
            case "mistral" -> new Mistral2LLMService(Region.US_WEST_2.toString(),
                                                      "mistral.mistral-large-2407-v1:0");
            default ->
                throw new IllegalArgumentException("Unknown LLM service type");
        };
    }
}
