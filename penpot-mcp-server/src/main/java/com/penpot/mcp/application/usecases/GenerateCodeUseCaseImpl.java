package com.penpot.mcp.application.usecases;

import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.in.*;
import com.penpot.mcp.core.ports.out.AiServicePort;
import com.penpot.mcp.infrastructure.factory.ContextEnrichmentChainFactory;
import com.penpot.mcp.infrastructure.chain.ContextEnricher;
import com.penpot.mcp.shared.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implémentation du use case de génération de code.
 * Orchestre l'enrichissement du contexte, la génération et l'exécution.
 * 
 * <h2>Responsabilités</h2>
 * <ul>
 *     <li>Validation des paramètres d'entrée</li>
 *     <li>Enrichissement du contexte via la chaîne de responsabilité</li>
 *     <li>Délégation de la génération à AiServicePort</li>
 *     <li>Exécution optionnelle du code généré</li>
 * </ul>
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
                return executeGeneratedCode(generatedCode, userToken);
            }

            return GenerateCodeResult.codeOnly(generatedCode);
        } catch (ValidationException | ToolExecutionException | TaskExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Code generation failed for task: {}", task, e);
            throw new ToolExecutionException(
                "Failed to generate code: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Exécute le code généré dans le plugin Penpot.
     * 
     * @param generatedCode code JavaScript généré
     * @param userToken     token utilisateur optionnel
     * @return résultat avec le code et l'exécution
     * @throws TaskExecutionException si l'exécution échoue
     */
    private GenerateCodeResult executeGeneratedCode(String generatedCode, String userToken) {
        log.debug("Executing generated code immediately");

        try {
            ExecuteCodeCommand command = ExecuteCodeCommand.builder()
                .code(generatedCode)
                .userToken(java.util.Optional.ofNullable(userToken))
                .build();

            TaskResult executionResult = executeCodeUseCase.execute(command);
            log.info("Code executed: success={}", executionResult.isSuccess());

            return GenerateCodeResult.withExecution(generatedCode, executionResult);
        } catch (Exception e) {
            log.error("Failed to execute generated code", e);

            if (e instanceof TaskExecutionException || 
                e instanceof TaskTimeoutException ||
                e instanceof PluginConnectionException) {
                throw e;
            }

            throw new TaskExecutionException(
                "Failed to execute generated code: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Valide les paramètres d'entrée.
     * 
     * @param task la tâche à valider
     * @throws ValidationException si les paramètres sont invalides
     */
    private void validateInput(String task) {
        if (task == null || task.isBlank()) {
            throw new ValidationException("Task cannot be null or empty");
        }

        if (task.length() > 5000) {
            throw new ValidationException(
                "Task description too long: " + task.length() + " characters (max 5000)"
            );
        }
    }
}