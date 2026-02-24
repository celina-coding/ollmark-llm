package com.penpot.ai.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Configuration du système RAG modulaire (Retrieval-Augmented Generation).
 *
 * <h2>Architecture RAG modulaire</h2>
 * <pre>
 * User Query
 *    │
 *    ▼
 * [RewriteQueryTransformer]   — reformule la query pour maximiser la pertinence
 *    │
 *    ▼
 * [MultiQueryExpander]        — génère N variantes sémantiques simultanément
 *    │
 *    ▼
 * [VectorStoreDocumentRetriever] — recherche par similarité cosinus
 *    │
 *    ▼
 * [ContextualQueryAugmenter]  — injecte le contexte dans le prompt final
 *    │
 *    ▼
 * Ollama LLM
 * </pre>
 *
 * <h2>Pourquoi le RAG modulaire ?</h2>
 * <p>Le {@code RagTemplateService} actuel effectue une recherche vectorielle directe
 * sur la query brute. Si l'utilisateur écrit "quelque chose de moderne pour une boulangerie",
 * la recherche peut rater les templates pertinents car la formulation ne correspond pas
 * aux embeddings des templates.</p>
 *
 * <p>Avec le RAG modulaire :</p>
 * <ul>
 *     <li>{@link RewriteQueryTransformer} : reformule en "template marketing moderne boulangerie"</li>
 *     <li>{@link MultiQueryExpander} : génère aussi "bakery social media post", "flyer artisan moderne"</li>
 *     <li>Les 3 requêtes sont cherchées en parallèle → union des résultats</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RagConfig {

    @Value("${penpot.ai.rag.similarity-threshold:0.5}")
    private double similarityThreshold;

    @Value("${penpot.ai.rag.top-k:5}")
    private int topK;

    @Value("${penpot.ai.rag.query-variants:3}")
    private int queryVariants;

    /**
     * VectorStore en mémoire.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("Initializing SimpleVectorStore (in-memory)");
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * {@link RetrievalAugmentationAdvisor} complet avec pipeline pré-retrieval.
     *
     * <h3>Pipeline</h3>
     * <ol>
     *     <li>{@link RewriteQueryTransformer} : reformule la query pour le vector store</li>
     *     <li>{@link MultiQueryExpander} : génère {@code queryVariants} variantes</li>
     *     <li>{@link VectorStoreDocumentRetriever} : recherche avec seuil de similarité</li>
     *     <li>{@link ContextualQueryAugmenter} : augmente le prompt avec les docs trouvés</li>
     * </ol>
     *
     * <p><b>allowEmptyContext=true</b> : si aucun template n'est trouvé (seuil non atteint),
     * l'IA peut quand même répondre avec ses connaissances générales au lieu de bloquer.</p>
     *
     * @param chatClientBuilder builder Spring AI pour les transformers (utilise un client dédié bas-temperature)
     * @param vectorStore       le store d'embeddings des templates
     * @return l'advisor RAG configuré
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
        ChatClient.Builder chatClientBuilder,
        VectorStore vectorStore
    ) {
        log.info("Configuring RetrievalAugmentationAdvisor:");
        log.info("  - similarityThreshold : {}", similarityThreshold);
        log.info("  - topK                : {}", topK);
        log.info("  - queryVariants       : {}", queryVariants);

        ChatClient.Builder transformerBuilder = chatClientBuilder
            .build()
            .mutate()
            .defaultOptions(
                OllamaChatOptions.builder()
                    .temperature(0.0)
                    .build()
            );

        // 1. Transformer : reformule la query utilisateur
        RewriteQueryTransformer rewriteTransformer = RewriteQueryTransformer.builder()
            .chatClientBuilder(transformerBuilder)
            .build();

        // 2. Expander : génère N variantes de la query
        MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
            .chatClientBuilder(transformerBuilder)
            .numberOfQueries(queryVariants)
            .includeOriginal(true)  // inclure la query originale en plus des variantes
            .build();

        // 3. Retriever : recherche vectorielle
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .similarityThreshold(similarityThreshold)
            .topK(topK)
            .build();

        // 4. Augmenter : injecte le contexte dans le prompt
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
            .allowEmptyContext(true)  // ne bloque pas si aucun template trouvé
            .build();

        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(rewriteTransformer)
            .queryExpander(multiQueryExpander)
            .documentRetriever(documentRetriever)
            .queryAugmenter(queryAugmenter)
            .build();
    }
}