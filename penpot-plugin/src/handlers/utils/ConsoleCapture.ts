
/**
 * Implémentation de Console qui capture tous les logs.
 * 
 * Responsabilité unique : Capturer et formater la sortie console.
 * 
 * Capture tous les niveaux de log :
 * - log, warn, error, info, debug, trace
 * - table, time, timeEnd
 * - group, groupCollapsed, groupEnd
 * - count, countReset, assert
 * 
 * @example
 * const capture = new ConsoleCapture();
 * capture.log("Hello", { name: "World" });
 * capture.error("Oops!");
 * const output = capture.getLog();
 * // "[LOG] Hello {\"name\":\"World\"}\n[ERROR] Oops!\n"
 */
export class ConsoleCapture {
    private logOutput: string = "";

    /** Réinitialise la capture */
    reset(): void {
        this.logOutput = "";
    }

    /** Récupère tous les logs capturés */
    getLog(): string {
        return this.logOutput;
    }

    /**
     * Ajoute un message au log avec formatage.
     * Les objets sont JSON.stringifiés avec indentation.
     */
    private appendToLog(level: string, ...args: any[]): void {
        const message = args
            .map((arg) => (typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg)))
            .join(" ");
        this.logOutput += `[${level}] ${message}\n`;
    }

    // Toutes les méthodes console standard
    readonly log   = (...args: any[]): void => this.appendToLog('LOG',   ...args);
    readonly warn  = (...args: any[]): void => this.appendToLog('WARN',  ...args);
    readonly error = (...args: any[]): void => this.appendToLog('ERROR', ...args);
    readonly info  = (...args: any[]): void => this.appendToLog('INFO',  ...args);
    readonly debug = (...args: any[]): void => this.appendToLog('DEBUG', ...args);
    readonly trace = (...args: any[]): void => this.appendToLog('TRACE', ...args);

    table(data: any): void                { this.appendToLog('TABLE',           data);                          }
    time(label?: string): void            { this.appendToLog('TIME',            `Timer started: ${label ?? 'default'}`); }
    timeEnd(label?: string): void         { this.appendToLog('TIME_END',        `Timer ended: ${label ?? 'default'}`);   }
    group(label?: string): void           { this.appendToLog('GROUP',           label ?? '');                   }
    groupCollapsed(label?: string): void  { this.appendToLog('GROUP_COLLAPSED', label ?? '');                   }
    groupEnd(): void                      { this.appendToLog('GROUP_END',       '');                            }
    count(label?: string): void           { this.appendToLog('COUNT',           label ?? 'default');            }
    countReset(label?: string): void      { this.appendToLog('COUNT_RESET',     label ?? 'default');            }
    clear(): void                         { /* capture intentionnelle, on ne vide pas */ }

    assert(condition: boolean, ...args: any[]): void {
        if (!condition) this.appendToLog('ASSERT', ...args);
    }
}