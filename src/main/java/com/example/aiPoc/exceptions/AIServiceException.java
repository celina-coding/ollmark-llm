package com.example.aiPoc.exceptions;

/**
 * Exception levée lors d'erreurs avec le service IA
 */
public class AIServiceException extends RuntimeException {
    
    private String provider;
    private String modelName;

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AIServiceException(String message, String provider, String modelName) {
        super(message);
        this.provider = provider;
        this.modelName = modelName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String toString() {
        return "AIServiceException{" +
                "message='" + getMessage() + '\'' +
                ", provider='" + provider + '\'' +
                ", modelName='" + modelName + '\'' +
                '}';
    }
}