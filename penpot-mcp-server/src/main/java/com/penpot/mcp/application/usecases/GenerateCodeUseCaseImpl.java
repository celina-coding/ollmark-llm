package com.penpot.mcp.application.usecases;

import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.in.*;
import com.penpot.mcp.core.ports.out.AiServicePort;
import com.penpot.mcp.infrastructure.factory.ContextEnrichmentChainFactory;
import com.penpot.mcp.infrastructure.chain.ContextEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implémentation du use case de génération de code.
 * Orchestre l'enrichissement du contexte, la génération et l'exécution.
 * Suit le Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateCodeUseCaseImpl implements GenerateCodeUseCase {

    private final AiServicePort aiService;
    private final ExecuteCodeUseCase executeCodeUseCase;
    private final ContextEnrichmentChainFactory chainFactory;

    @Override
    public GenerateCodeResult generate(
        String task, 
        String context, 
        String userToken,
        boolean executeImmediately
    ) {
        log.info("Generating code for task: {} (execute: {})", task, executeImmediately);
        validateInput(task);

        try {
            AiContext initialContext = AiContext.forCodeGeneration(task, context);

            ContextEnricher chain = chainFactory.createChain();
            AiContext enrichedContext = chain.enrich(initialContext);

            log.debug("Context enriched with {} API docs, {} examples, {} best practices",
                enrichedContext.getApiDocumentation().size(),
                enrichedContext.getExamples().size(),
                enrichedContext.getBestPractices().size());

            String generatedCode = aiService.generateCode(enrichedContext);
            log.info("Code generated successfully (length: {} chars)", 
                generatedCode.length());

            if (executeImmediately) {
                log.debug("Executing generated code immediately");

                ExecuteCodeCommand command = ExecuteCodeCommand.builder()
                    .code(generatedCode)
                    .userToken(java.util.Optional.ofNullable(userToken))
                    .build();

                TaskResult executionResult = executeCodeUseCase.execute(command);
                log.info("Code executed: success={}", executionResult.isSuccess());
                return GenerateCodeResult.withExecution(generatedCode, executionResult);
            }

            return GenerateCodeResult.codeOnly(generatedCode);
        } catch (Exception e) {
            log.error("Code generation failed", e);
            throw new RuntimeException(
                "Failed to generate code: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Valide les paramètres d'entrée.
     */
    private void validateInput(String task) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("Task cannot be null or empty");
        }

        if (task.length() > 5000) {
            throw new IllegalArgumentException(
                "Task description too long (max 5000 characters)"
            );
        }
    }
}