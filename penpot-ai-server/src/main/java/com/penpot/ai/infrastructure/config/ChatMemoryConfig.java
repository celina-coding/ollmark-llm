package com.penpot.ai.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.memory.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

/**
 * Configuration de la mémoire de conversation (Chat Memory).
 * 
 * Configuration depuis application.yml :
 * - penpot.chat.memory.max-messages : Nombre max de messages en mémoire
 * - spring.ai.chat.memory.repository.jdbc.initialize-schema : Initialisation schéma
 * - spring.datasource.* : Configuration de la base de données
 * 
 * @see <a href="https://docs.spring.ai/reference/api/chatmemory.html">Spring AI Chat Memory</a>
 */
@Slf4j
@Configuration
public class ChatMemoryConfig {

    /**
     * Nombre maximum de messages dans la fenêtre de mémoire.
     */
    @Value("${penpot.ai.chat.memory.max-messages}")
    private int maxMessages;

    @Autowired
    ChatMemoryRepository chatMemoryRepository;

    /**
     * Configure le ChatMemory avec MessageWindowChatMemory.
     * 
     * @param chatMemoryRepository repository auto-configuré par Spring AI
     * @return le ChatMemory configuré
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        log.info("==============================================");
        log.info("Configuring ChatMemory with MessageWindowChatMemory");
        log.info("Max messages in memory window: {}", maxMessages);
        log.info("Repository type: {}", chatMemoryRepository.getClass().getSimpleName());
        log.info("==============================================");

        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(maxMessages)
            .build();
    }

    /**
     * Configure le MessageChatMemoryAdvisor pour le ChatClient.
     * 
     * @param chatMemory le ChatMemory configuré
     * @return l'advisor configuré
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        log.info("Configuring MessageChatMemoryAdvisor");
        log.info("Automatic conversation history management enabled");
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}