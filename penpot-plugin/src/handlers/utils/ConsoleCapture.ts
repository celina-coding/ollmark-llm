/**
 * Console implementation that captures all log output
 * Single Responsibility: Capture and format console output
 */
export class ConsoleCapture {
    private logOutput: string = "";

    reset(): void {
        this.logOutput = "";
    }

    getLog(): string {
        return this.logOutput;
    }

    private appendToLog(level: string, ...args: any[]): void {
        const message = args
            .map((arg) => (typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg)))
            .join(" ");
        this.logOutput += `[${level}] ${message}\n`;
    }

    log(...args: any[]): void {
        this.appendToLog("LOG", ...args);
    }

    warn(...args: any[]): void {
        this.appendToLog("WARN", ...args);
    }

    error(...args: any[]): void {
        this.appendToLog("ERROR", ...args);
    }

    info(...args: any[]): void {
        this.appendToLog("INFO", ...args);
    }

    debug(...args: any[]): void {
        this.appendToLog("DEBUG", ...args);
    }

    trace(...args: any[]): void {
        this.appendToLog("TRACE", ...args);
    }

    table(data: any): void {
        this.appendToLog("TABLE", data);
    }

    time(label?: string): void {
        this.appendToLog("TIME", `Timer started: ${label || "default"}`);
    }

    timeEnd(label?: string): void {
        this.appendToLog("TIME_END", `Timer ended: ${label || "default"}`);
    }

    group(label?: string): void {
        this.appendToLog("GROUP", label || "");
    }

    groupCollapsed(label?: string): void {
        this.appendToLog("GROUP_COLLAPSED", label || "");
    }

    groupEnd(): void {
        this.appendToLog("GROUP_END", "");
    }

    clear(): void {
        // Intentionally empty - we want to capture logs
    }

    count(label?: string): void {
        this.appendToLog("COUNT", label || "default");
    }

    countReset(label?: string): void {
        this.appendToLog("COUNT_RESET", label || "default");
    }

    assert(condition: boolean, ...args: any[]): void {
        if (!condition) {
            this.appendToLog("ASSERT", ...args);
        }
    }
}