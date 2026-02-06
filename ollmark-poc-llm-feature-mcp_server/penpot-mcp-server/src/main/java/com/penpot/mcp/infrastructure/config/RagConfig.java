package com.penpot.mcp.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;
import org.springframework.context.annotation.*;

/**
 * Configuration pour le système RAG (Retrieval-Augmented Generation).
 * Configure le VectorStore pour stocker et rechercher les embeddings.
 * Utilise SimpleVectorStore en mémoire pour simplicité et performances.
 */
@Configuration
public class RagConfig {

    /**
     * VectorStore en mémoire pour stocker les embeddings des templates.
     * Simple et ne nécessite aucune base de données externe.
     * Parfait pour un nombre limité de templates marketing.
     * 
     * @param embeddingModel le modèle d'embedding Ollama injecté automatiquement
     * @return le vector store configuré
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}