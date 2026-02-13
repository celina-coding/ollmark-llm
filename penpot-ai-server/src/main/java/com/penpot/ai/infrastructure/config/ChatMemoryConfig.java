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
 * Implémente le système de mémoire persistante recommandé par Spring AI.
 * Utilise JdbcChatMemoryRepository pour la persistence en base de données.
 * 
 * Configuration depuis application.yml :
 * - penpot.chat.memory.max-messages : Nombre max de messages en mémoire
 * - spring.ai.chat.memory.repository.jdbc.initialize-schema : Initialisation schéma
 * - spring.datasource.* : Configuration de la base de données
 * 
 * Principes appliqués :
 * - Single Responsibility : Configuration dédiée à la mémoire
 * - Dependency Inversion : Utilise les abstractions Spring AI
 * - Open/Closed : Extensible via injection de repository custom
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
     * MessageWindowChatMemory maintient une fenêtre glissante de N messages
     * les plus récents, tout en préservant les messages système.
     * 
     * Auto-configuration Spring AI :
     * - Si JdbcChatMemoryRepository est présent (dépendance ajoutée dans pom.xml), 
     *   il sera auto-configuré et injecté automatiquement
     * - Le schéma SQL est créé automatiquement selon la configuration
     * - Supporte H2, PostgreSQL, MySQL, SQL Server, Oracle, etc.
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
     * MessageChatMemoryAdvisor gère automatiquement :
     * - Récupération de l'historique avant chaque appel AI
     * - Sauvegarde du message utilisateur dans l'historique
     * - Sauvegarde de la réponse AI dans l'historique
     * - Gestion de la fenêtre de messages (garde les N derniers)
     * 
     * Architecture :
     * 1. User envoie un message avec conversationId
     * 2. Advisor récupère l'historique depuis ChatMemory
     * 3. Historique + nouveau message → envoyé à l'AI
     * 4. Réponse AI → sauvegardée dans ChatMemory
     * 5. Retour de la réponse à l'utilisateur
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

    /**
     * Bean pour obtenir des statistiques sur la mémoire (optionnel).
     * Utile pour monitoring et debugging.
     * 
     * @param chatMemory le ChatMemory configuré
     * @return le service de statistiques
     */
    @Bean
    public ChatMemoryStatsService chatMemoryStatsService(ChatMemory chatMemory) {
        return new ChatMemoryStatsService(chatMemory);
    }

    /**
     * Service utilitaire pour obtenir des statistiques sur ChatMemory.
     */
    public static class ChatMemoryStatsService {
        private final ChatMemory chatMemory;

        public ChatMemoryStatsService(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        /**
         * Obtient le nombre de messages pour une conversation.
         * 
         * @param conversationId ID de la conversation
         * @return nombre de messages
         */
        public int getMessageCount(String conversationId) {
            return chatMemory.get(conversationId).size();
        }

        /**
         * Log les statistiques d'une conversation.
         * 
         * @param conversationId ID de la conversation
         */
        public void logConversationStats(String conversationId) {
            int messageCount = getMessageCount(conversationId);
            log.info("Conversation {} has {} messages in history", 
                conversationId, messageCount);
        }
    }
}