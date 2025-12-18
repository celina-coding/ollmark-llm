
export interface ExecutionResult {
    success: boolean;
    error?: string;
}

export interface PageExportResult {
    success: boolean;
    pageData?: any;
    error?: string;
}

export interface PenpotMessage {
    type: string;
    code?: string;
    pageId?: string;
}