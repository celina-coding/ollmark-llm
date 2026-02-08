import { IApiService } from '../interfaces/IUIComponents';

/**
 * Service d'appels API HTTP.
 * 
 * **Endpoints gérés** :
 * - `/ai/chat/new` : Création de conversation
 * - `/ai/chat` : Envoi de messages
 * - `/ai/execute-code` : Exécution de code JavaScript
 * 
 * **Gestion d'erreurs robuste** :
 * - Parse les réponses JSON même en cas d'erreur
 * - Extrait les messages d'erreur du backend
 * - Fournit des fallbacks pour toutes les opérations
 * 
 * @implements {IApiService}
 * 
 * @example
 * ```typescript
 * const logger = { log: (msg) => console.log(msg) };
 * const apiService = new ApiService('http://localhost:4401', logger);
 * 
 * // Créer une conversation
 * const { conversationId } = await apiService.newConversation();
 * 
 * // Envoyer un message
 * const { response } = await apiService.sendMessage(
 *   conversationId,
 *   'Bonjour !'
 * );
 * 
 * // Exécuter du code
 * const result = await apiService.executeCode('return 42;');
 * console.log(result.result); // 42
 * ```
 */
export class ApiService implements IApiService {
    /**
     * Crée une instance du service API.
     * 
     * @param baseUrl - URL de base du backend (ex: 'http://localhost:4401')
     * @param logger - Logger optionnel pour tracer les opérations
     * 
     * @example
     * ```typescript
     * const api = new ApiService(
     *   'http://localhost:4401',
     *   { log: (msg) => console.log(`[API] ${msg}`) }
     * );
     * ```
     */
    constructor(
        private readonly baseUrl: string,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    /**
     * Crée une nouvelle conversation avec l'IA.
     * 
     * **Endpoint** : `POST /ai/chat/new`
     * 
     * **Processus** :
     * 1. Envoie une requête POST vide
     * 2. Reçoit un ID de conversation
     * 3. Valide la présence de l'ID
     * 4. Retourne l'ID pour usage ultérieur
     * 
     * @returns Promise contenant l'ID de la conversation créée
     * @throws {Error} Si le backend ne retourne pas de conversationId
     * @throws {Error} Si la requête HTTP échoue
     * 
     * @example
     * ```typescript
     * try {
     *   const { conversationId } = await apiService.newConversation();
     *   console.log('Conversation créée:', conversationId);
     *   // conversationId: "conv_abc123def456"
     * } catch (error) {
     *   console.error('Échec création:', error.message);
     * }
     * ```
     */
    async newConversation(): Promise<{ conversationId: string }> {
        this.log('Création nouvelle conversation...');

        const data = await this.postJson<{ conversationId?: string }>(
            `${this.baseUrl}/ai/chat/new`,
            {}
        );

        if (!data.conversationId) {
            throw new Error('Aucun conversationId dans la réponse');
        }

        this.log(`✓ Conversation créée: ${data.conversationId}`);
        return { conversationId: data.conversationId };
    }

    /**
     * Envoie un message dans une conversation existante.
     * 
     * **Endpoint** : `POST /ai/chat`
     * 
     * **Payload** :
     * ```json
     * {
     *   "conversationId": "conv_abc123",
     *   "message": "Bonjour !"
     * }
     * ```
     * 
     * **Réponse** :
     * ```json
     * {
     *   "response": "Bonjour ! Comment puis-je vous aider ?"
     * }
     * ```
     * 
     * @param conversationId - ID de la conversation (de `newConversation()`)
     * @param message - Message utilisateur à envoyer
     * @returns Promise contenant la réponse de l'IA
     * 
     * @example
     * ```typescript
     * const { response } = await apiService.sendMessage(
     *   conversationId,
     *   'Explique-moi les closures en JavaScript'
     * );
     * 
     * console.log(response);
     * // "Une closure est une fonction qui..."
     * ```
     */
    async sendMessage(conversationId: string, message: string): Promise<{ response: string }> {
        this.log(`Envoi message (${message.length} chars)...`);

        const data = await this.postJson<{ response?: string }>(
            `${this.baseUrl}/ai/chat`,
            { conversationId, message }
        );

        this.log('✓ Réponse reçue');
        return { response: data.response ?? '' };
    }

    /**
     * Exécute du code JavaScript côté serveur.
     * 
     * **Endpoint** : `POST /ai/execute-code`
     * 
     * **Payload** :
     * ```json
     * {
     *   "code": "const x = 5; return x * 2;"
     * }
     * ```
     * 
     * **Réponse** :
     * ```json
     * {
     *   "success": true,
     *   "result": 10,
     *   "logs": "[LOG] Exécution réussie\n"
     * }
     * ```
     * 
     * **Fonctionnalités** :
     * - Exécution dans un environnement isolé
     * - Capture des logs console
     * - Retour du résultat de l'exécution
     * - Gestion des erreurs d'exécution
     * 
     * @param code - Code JavaScript à exécuter
     * @returns Promise avec succès, résultat et logs
     * 
     * @example
     * ```typescript
     * const result = await apiService.executeCode(`
     *   console.log('Hello');
     *   const sum = 40 + 2;
     *   return sum;
     * `);
     * 
     * console.log(result.success); // true
     * console.log(result.result);  // 42
     * console.log(result.logs);    // "[LOG] Hello\n"
     * ```
     */
    async executeCode(code: string): Promise<{ success: boolean; logs?: string; result?: any }> {
        this.log(`Exécution JavaScript (${code.length} chars)...`);

        const data = await this.postJson<{ success?: boolean; logs?: string; result?: any }>(
            `${this.baseUrl}/ai/execute-code`,
            { code }
        );

        this.log(`✓ Exécution terminée (success=${data.success})`);
        return {
            success: data.success ?? false,
            logs: data.logs,
            result: data.result
        };
    }

    /**
     * Effectue une requête POST JSON.
     * 
     * **Processus** :
     * 1. Envoie la requête POST avec headers JSON
     * 2. Lit la réponse en texte brut
     * 3. Parse le JSON (avec gestion d'erreur)
     * 4. Vérifie le statut HTTP
     * 5. Extrait et lance les erreurs si nécessaire
     * 6. Retourne les données parsées
     * 
     * **Gestion d'erreurs** :
     * - Parse JSON même si vide ou invalide
     * - Extrait les messages d'erreur du backend
     * - Fournit des messages par défaut (status + statusText)
     * - Log les erreurs de parsing JSON
     * 
     * @template T - Type de la réponse attendue
     * @param url - URL complète de l'endpoint
     * @param body - Corps de la requête (sera JSON.stringify)
     * @returns Promise des données parsées
     * @throws {Error} Si la requête échoue (status non-OK)
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // Usage interne dans les méthodes publiques
     * const data = await this.postJson<ResponseType>(
     *   'http://api.com/endpoint',
     *   { param: 'value' }
     * );
     * ```
     */
    private async postJson<T>(url: string, body: any): Promise<T> {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const text = await response.text();
        let data: any = {};

        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            this.log(`Erreur parsing JSON: ${e}`);
        }

        if (!response.ok) {
            throw new Error(data?.error || `${response.status} ${response.statusText}`);
        }

        return data as T;
    }

    /**
     * Enregistre un message via le logger optionnel.
     * 
     * @param message - Message à logger
     * @private
     */
    private log(message: string): void {
        if (this.logger) {
            this.logger.log(message);
        }
    }
}