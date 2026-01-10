package com.penpot.mcp.tools;

import com.penpot.mcp.model.*;
import com.penpot.mcp.service.PluginBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Outil pour l'exécution de code JavaScript dans le plugin Penpot.
 * <p>
 * Cet outil fournit une interface de haut niveau pour:
 * </p>
 * <ul>
 *   <li>Vérifier la disponibilité d'une connexion plugin</li>
 *   <li>Exécuter du code JavaScript dans le contexte du plugin</li>
 *   <li>Formater et structurer les résultats en JSON</li>
 *   <li>Gérer les logs d'exécution</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteCodeTool {

    /** Service pont pour communiquer avec le plugin */
    private final PluginBridge pluginBridge;

    /**
     * Timeout pour l'exécution des tâches en secondes.
     * Configurable via {@code penpot.mcp.task-timeout-seconds}, défaut: 30s.
     */
    @Value("${penpot.mcp.task-timeout-seconds:30}")
    private int taskTimeoutSeconds;

    /**
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     * <p>
     * Le processus est le suivant:
     * </p>
     * <ol>
     *   <li>Vérifie qu'une connexion plugin est active</li>
     *   <li>Envoie le code au plugin via {@link PluginBridge}</li>
     *   <li>Attend le résultat avec timeout configuré</li>
     *   <li>Formate le résultat en JSON structuré</li>
     * </ol>
     * <p>
     * Le JSON retourné a la structure:
     * </p>
     * <pre>
     * {
     *   "result": &lt;valeur retournée par le code&gt;,
     *   "log": "&lt;sortie console si présente&gt;"
     * }
     * </pre>
     *
     * @param code le code JavaScript à exécuter
     * @param userToken token optionnel pour le mode multi-utilisateur
     * @return le résultat formaté en JSON
     * @throws IllegalStateException si aucune connexion plugin n'est active
     * @throws Exception si l'exécution échoue
     */
    public String execute(String code, String userToken) throws Exception {
        if (!pluginBridge.hasActiveConnection()) {
            throw new IllegalStateException(
                "No active Penpot plugin connection. Please ensure the plugin is loaded and connected."
            );
        }

        log.info("Executing code in Penpot plugin (length: {} chars)", code.length());

        Map<String, Object> params = Map.of("code", code);
        PluginTaskResponse<ExecuteCodeTaskResultData<Object>> response = 
            pluginBridge.executeTask("executeCode", params, userToken, taskTimeoutSeconds);

        ExecuteCodeTaskResultData<Object> data = response.getData();
        if (data == null) return "{}";

        StringBuilder resultJson = new StringBuilder();
        resultJson.append("{\n");

        if (data.getResult() != null) {
            resultJson.append("  \"result\": ");
            resultJson.append(formatResult(data.getResult()));
            resultJson.append(",\n");
        }

        if (data.getLog() != null && !data.getLog().isEmpty()) {
            resultJson.append("  \"log\": ");
            resultJson.append(escapeJson(data.getLog()));
            resultJson.append("\n");
        }

        if (data.getLog() == null || data.getLog().isEmpty()) {
            int lastComma = resultJson.lastIndexOf(",");
            if (lastComma > 0) {
                resultJson.delete(lastComma, lastComma + 1);
                resultJson.append("\n");
            }
        }
        resultJson.append("}");

        log.info("Code execution completed successfully");
        return resultJson.toString();
    }

    /**
     * Formate un résultat en représentation JSON appropriée.
     * <p>
     * Gère les types:
     * </p>
     * <ul>
     *   <li>null → "null"</li>
     *   <li>String → chaîne échappée avec guillemets</li>
     *   <li>Number/Boolean → toString() direct</li>
     *   <li>Autres → toString()</li>
     * </ul>
     *
     * @param result l'objet résultat à formater
     * @return la représentation JSON du résultat
     */
    private String formatResult(Object result) {
        if (result == null) return "null";
        if (result instanceof String) {
            return escapeJson((String) result);
        }
        if (result instanceof Number || result instanceof Boolean) {
            return result.toString();
        }
        return result.toString();
    }

    /**
     * Échappe une chaîne pour inclusion dans un JSON.
     * <p>
     * Gère les caractères spéciaux:
     * </p>
     * <ul>
     *   <li>\ → \\</li>
     *   <li>" → \"</li>
     *   <li>newline → \n</li>
     *   <li>carriage return → \r</li>
     *   <li>tab → \t</li>
     * </ul>
     *
     * @param str la chaîne à échapper
     * @return la chaîne échappée entre guillemets, ou "null" si str est null
     */
    private String escapeJson(String str) {
        if (str == null) return "null";
        return "\"" + str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        + "\"";
    }
}