package org.example.llm;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public interface LLMService {
    String sendMessages(List<ChatMessage> messages);

}
