package com.example.aiPoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration principale du provider d'IA utilisé dans l'application.
 * <p>
 * Cette classe charge automatiquement les paramètres définis
 * dans le fichier de configuration Spring (ex. {@code application.yml})
 * sous le préfixe {@code spring.ai.openai}.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.openai")
public class AIProviderConfig {

    private String apiKey;
    private String baseUrl;
    private ChatOptions chat = new ChatOptions();

    public static class ChatOptions {
        private OptionsDetail options = new OptionsDetail();

        public static class OptionsDetail {
            private String model;
            private Integer maxTokens = 1000;
            private Double temperature = 0.7;

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public Integer getMaxTokens() {
                return maxTokens;
            }

            public void setMaxTokens(Integer maxTokens) {
                this.maxTokens = maxTokens;
            }

            public Double getTemperature() {
                return temperature;
            }

            public void setTemperature(Double temperature) {
                this.temperature = temperature;
            }
        }

        public OptionsDetail getOptions() {
            return options;
        }

        public void setOptions(OptionsDetail options) {
            this.options = options;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ChatOptions getChat() {
        return chat;
    }

    public void setChat(ChatOptions chat) {
        this.chat = chat;
    }

    public String getModelName() {
        return chat.getOptions().getModel();
    }
}