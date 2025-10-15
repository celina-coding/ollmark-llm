package com.example.aiPoc.Services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class AIService {
    private final ChatClient chatClient;

    public AIService(OpenAiChatModel chatModel){
        this.chatClient = ChatClient.builder(chatModel).build();
    }
    public String chat(String prompt){
        return chatClient.prompt(prompt).call().content();
    }
}
