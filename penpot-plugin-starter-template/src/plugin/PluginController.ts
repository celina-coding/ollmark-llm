import { MessageHandler } from "./handlers";
import { CodeExecutor, PageExporter, StateMessenger } from "./services"
import { PenpotMessage } from "./types";

export class PluginController {
    private messageHandler: MessageHandler;
    private stateMessenger: StateMessenger;

    constructor() {
        const codeExecutor = new CodeExecutor();
        const pageExporter = new PageExporter();
        this.stateMessenger = new StateMessenger();

        this.messageHandler = new MessageHandler(
            codeExecutor,
            pageExporter,
            this.stateMessenger
        );

        this.initialize();
    }

    private initialize(): void {
        this.openUI();
        this.setupMessageListener();
        this.setupEventListeners();
        this.sendInitialState();
    }

    private openUI(): void {
        penpot.ui.open("AI Code Generator", `?theme=${penpot.theme}`, {
            width: 600,
            height: 750
        });
    }

    private setupMessageListener(): void {
        penpot.ui.onMessage<PenpotMessage>(async (message) => {
            await this.messageHandler.handle(message);
        });
    }

    private setupEventListeners(): void {
        penpot.on("themechange", (theme) => {
            this.stateMessenger.sendThemeChange(theme);
        });

        penpot.on("filechange", () => {
            this.stateMessenger.sendCurrentState();
        });

        penpot.on("pagechange", () => {
            this.stateMessenger.sendCurrentState();
        });
    }

    private sendInitialState(): void {
        setTimeout(() => {
            this.stateMessenger.sendCurrentState();
        }, 100);
    }
}