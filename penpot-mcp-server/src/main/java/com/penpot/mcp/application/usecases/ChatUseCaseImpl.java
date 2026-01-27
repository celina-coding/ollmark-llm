package com.penpot.mcp.application.usecases;

import com.penpot.mcp.core.ports.in.ChatUseCase;
import com.penpot.mcp.core.ports.out.AiServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Implémentation du use case de chat.
 * Délègue au service AI via le port approprié.
 * Suit le Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUseCaseImpl implements ChatUseCase {

    private final AiServicePort aiService;

    @Override
    public String chat(String message, List<Message> conversationHistory) {
        log.info("Processing chat request (message length: {} chars, history: {} messages)", 
            message.length(),
            conversationHistory != null ? conversationHistory.size() : 0);

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        try {
            String response = aiService.chat(message, conversationHistory);

            log.info("Chat completed successfully (response length: {} chars)", 
                response.length());

            return response;
        } catch (Exception e) {
            log.error("Chat failed", e);
            throw new RuntimeException("Failed to process chat: " + e.getMessage(), e);
        }
    }
}