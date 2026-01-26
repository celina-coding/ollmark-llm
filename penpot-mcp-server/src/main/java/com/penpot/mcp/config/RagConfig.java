package com.penpot.mcp.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour le système RAG avec VectorStore en mémoire.
 */
@Configuration
public class RagConfig {
    
    /**
     * VectorStore en mémoire pour stocker les embeddings des templates.
     * Simple et ne nécessite aucune base de données externe.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}