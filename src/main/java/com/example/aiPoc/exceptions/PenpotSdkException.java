package com.example.aiPoc.exceptions;

/**
 * Exception levée lors d'erreurs liées au SDK Penpot
 */
public class PenpotSdkException extends RuntimeException {
    
    private String methodName;
    private String apiVersion;

    public PenpotSdkException(String message) {
        super(message);
    }

    public PenpotSdkException(String message, Throwable cause) {
        super(message, cause);
    }

    public PenpotSdkException(String message, String methodName) {
        super(message);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String toString() {
        return "PenpotSdkException{" +
                "message='" + getMessage() + '\'' +
                ", methodName='" + methodName + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                '}';
    }
}