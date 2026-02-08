package com.penpot.ai.infrastructure.factory;

import com.penpot.ai.infrastructure.strategy.ResultFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Factory pour sélectionner le formatter approprié selon le type de résultat.
 * Applique le Strategy Pattern avec sélection automatique.
 */
@Slf4j
@Component
public class ResultFormatterFactory {

    private final List<ResultFormatter> formatters;
    private final ResultFormatter defaultFormatter;

    /**
     * Construit la factory avec tous les formatters disponibles.
     * Spring injecte automatiquement tous les beans ResultFormatter.
     * 
     * @param formatters liste de tous les formatters enregistrés
     */
    public ResultFormatterFactory(List<ResultFormatter> formatters) {
        // Trier par priorité décroissante
        this.formatters = formatters.stream()
            .sorted(Comparator.comparingInt(ResultFormatter::priority).reversed())
            .toList();

        // Le dernier (priorité la plus basse) devient le formatter par défaut
        this.defaultFormatter = this.formatters.isEmpty() 
            ? createFallbackFormatter() 
            : this.formatters.get(this.formatters.size() - 1);

        log.info("Initialized ResultFormatterFactory with {} formatters", formatters.size());
    }

    /**
     * Obtient le formatter pour un objet (utilise getClass()).
     * 
     * @param result l'objet à formater
     * @return le formatter approprié
     */
    public ResultFormatter getFormatterForObject(Object result) {
        if (result == null) return defaultFormatter;
        return getFormatter(result.getClass());
    }

    /**
     * Obtient le formatter le plus approprié pour le type donné.
     * 
     * @param resultType le type de résultat à formater
     * @return le formatter avec la plus haute priorité qui supporte ce type
     */
    private ResultFormatter getFormatter(Class<?> resultType) {
        if (resultType == null) return defaultFormatter;
        return formatters.stream()
            .filter(formatter -> formatter.supports(resultType))
            .findFirst()
            .orElseGet(() -> {
                log.warn("No specific formatter found for type {}, using default", 
                    resultType.getSimpleName());
                return defaultFormatter;
            });
    }

    /**
     * Crée un formatter de secours en cas d'absence de formatters configurés.
     */
    private ResultFormatter createFallbackFormatter() {
        return new ResultFormatter() {
            @Override
            public String format(Object result) {
                return result != null ? result.toString() : "";
            }

            @Override
            public boolean supports(Class<?> resultType) {
                return true;
            }

            @Override
            public int priority() {
                return -1;
            }
        };
    }
}