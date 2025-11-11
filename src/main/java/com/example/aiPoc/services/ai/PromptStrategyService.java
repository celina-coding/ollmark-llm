package com.example.aiPoc.services.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.PromptStrategy;

/**
 * Service responsable de la gestion et de l’optimisation des stratégies de génération de prompts.
 *
 * <p>
 * Ce service analyse les performances de chaque {@link PromptStrategy} afin d’adapter la sélection
 * automatique de stratégie en fonction du contexte et de l’historique des résultats.  
 * Il permet également de mesurer et de suivre l’efficacité des stratégies utilisées
 * dans la génération de code par le modèle d’intelligence artificielle.
 * </p>
 *
 * <p><b>Principales responsabilités :</b></p>
 * <ul>
 *   <li>Sélection de la stratégie la plus appropriée selon le prompt utilisateur.</li>
 *   <li>Analyse et enregistrement des performances de chaque stratégie (taux de succès, temps, longueur de code).</li>
 *   <li>Recommandations adaptatives basées sur l’historique des résultats.</li>
 * </ul>
 * 
 * @see PromptStrategy
 */
@Service
public class PromptStrategyService {

    /** Logger principal pour le suivi et la traçabilité des opérations. */
    private static final Logger logger = LoggerFactory.getLogger(PromptStrategyService.class);
    
    /** Historique des performances associées à chaque stratégie de prompt. */
    private final Map<PromptStrategy, StrategyMetrics> metricsHistory = new HashMap<>();

    /**
     * Constructeur par défaut.  
     * Initialise les métriques pour toutes les stratégies disponibles définies dans {@link PromptStrategy}.
     */
    public PromptStrategyService() {
        for (PromptStrategy strategy : PromptStrategy.values()) {
            metricsHistory.put(strategy, new StrategyMetrics(strategy));
        }
    }

    /**
     * Enregistre les résultats d’une génération afin d’améliorer l’apprentissage statistique
     * des performances de chaque stratégie.
     *
     * @param strategy   la stratégie utilisée pour la génération
     * @param success    indique si la génération a été réussie
     * @param durationMs la durée d’exécution en millisecondes
     * @param codeLength la longueur du code généré
     */
    public void recordResult(PromptStrategy strategy, boolean success, long durationMs, int codeLength) {
        StrategyMetrics metrics = metricsHistory.get(strategy);
        metrics.recordAttempt(success, durationMs, codeLength);

        logger.debug("Résultat enregistré pour {}: success={}, duration={}ms", 
                    strategy, success, durationMs);
    }

    /**
     * Retourne une copie de l’ensemble des métriques enregistrées pour toutes les stratégies.
     *
     * @return une map contenant toutes les métriques par stratégie
     */
    public Map<PromptStrategy, StrategyMetrics> getAllMetrics() {
        return new HashMap<>(metricsHistory);
    }

    /**
     * Classe interne représentant les métriques de performance associées à une stratégie donnée.
     *
     * <p>
     * Chaque instance stocke des informations statistiques comme le nombre total de tentatives,
     * la durée moyenne, la longueur moyenne du code et le taux de succès.
     * </p>
     */
    public static class StrategyMetrics {
        /** Stratégie associée à ces métriques. */
        private final PromptStrategy strategy;

        /** Nombre total de tentatives effectuées. */
        private int totalAttempts = 0;

        /** Nombre de tentatives réussies. */
        private int successfulAttempts = 0;

        /** Durée totale cumulée de toutes les générations. */
        private long totalDurationMs = 0;

        /** Longueur totale cumulée du code généré. */
        private int totalCodeLength = 0;

        /** Liste circulaire des durées les plus récentes (10 dernières). */
        private final List<Long> recentDurations = new ArrayList<>();

        /**
         * Crée une nouvelle instance de métriques pour une stratégie donnée.
         *
         * @param strategy la stratégie concernée
         */
        public StrategyMetrics(PromptStrategy strategy) {
            this.strategy = strategy;
        }

        /**
         * Enregistre une tentative de génération et met à jour les statistiques associées.
         *
         * @param success    indique si la tentative a réussi
         * @param durationMs durée de la génération en millisecondes
         * @param codeLength longueur du code généré
         */
        public void recordAttempt(boolean success, long durationMs, int codeLength) {
            totalAttempts++;
            if (success) successfulAttempts++;
            totalDurationMs += durationMs;
            totalCodeLength += codeLength;

            recentDurations.add(durationMs);
            if (recentDurations.size() > 10) {
                recentDurations.remove(0);
            }
        }

        /**
         * Calcule le taux de succès de la stratégie.
         *
         * @return le taux de réussite entre 0.0 et 1.0
         */
        public double getSuccessRate() {
            return totalAttempts > 0 ? (double) successfulAttempts / totalAttempts : 0.0;
        }

        /**
         * Retourne la durée moyenne d’exécution.
         *
         * @return la durée moyenne en millisecondes
         */
        public double getAverageDurationMs() {
            return totalAttempts > 0 ? (double) totalDurationMs / totalAttempts : 0.0;
        }

        /**
         * Retourne la longueur moyenne du code généré.
         *
         * @return la longueur moyenne du code
         */
        public double getAverageCodeLength() {
            return totalAttempts > 0 ? (double) totalCodeLength / totalAttempts : 0.0;
        }

        /** @return la stratégie concernée */
        public PromptStrategy getStrategy() {
            return strategy;
        }

        /** @return le nombre total de tentatives enregistrées */
        public int getTotalAttempts() {
            return totalAttempts;
        }

        /** @return le nombre de tentatives réussies */
        public int getSuccessfulAttempts() {
            return successfulAttempts;
        }

        /** @return la durée totale cumulée en millisecondes */
        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        /** @return la longueur totale cumulée du code généré */
        public int getTotalCodeLength() {
            return totalCodeLength;
        }

        /**
         * Retourne une copie de la liste des durées récentes enregistrées.
         *
         * @return une liste contenant les 10 dernières durées de génération
         */
        public List<Long> getRecentDurations() {
            return new ArrayList<>(recentDurations);
        }

        @Override
        public String toString() {
            return String.format(
                "StrategyMetrics{strategy=%s, attempts=%d, successRate=%.2f%%, avgDuration=%.0fms}",
                strategy, totalAttempts, getSuccessRate() * 100, getAverageDurationMs()
            );
        }
    }
}