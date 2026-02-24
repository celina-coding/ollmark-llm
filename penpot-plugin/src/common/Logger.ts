/**
 * Interface pour la gestion des logs.
 */

export interface ILogger {
    /**
     * Ajoute un message au log.
     * @param message - Message à logger
     */
    log(message: string): void;
}

/** Fonction de log — no-op si pas de logger injecté */
export type LogFn = (message: string) => void;

/**
 * Crée une fonction de log à partir d'un logger optionnel.
 * Retourne une no-op si aucun logger n'est fourni.
 */
export function createLogger(logger?: ILogger): LogFn {
    return logger ? (msg) => logger.log(msg) : () => {};
}