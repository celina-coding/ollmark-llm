package com.penpot.ai.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class OllamaTokenLoggingConfig {

    @Value("${spring.ai.ollama.chat.options.num_ctx}")
    private int numCtx;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Personnalise le RestClient utilisé par Spring AI (OllamaApi).
     * Spring AI Ollama autoconfig prend un RestClient.Builder si présent. :contentReference[oaicite:1]{index=1}
     */
    @Bean
    public RestClientCustomizer ollamaRestClientTokenLogger() {
        return builder -> builder
            // Important: permet de relire le body après interception.
            .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
            .requestInterceptor(this::logTokenMetricsInterceptor);
    }

    private ClientHttpResponse logTokenMetricsInterceptor(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {

        ClientHttpResponse response = execution.execute(request, body);

        // On lit la réponse en bytes (buffering activé ci-dessus), sans la "consommer" définitivement.
        byte[] responseBytes = response.getBody().readAllBytes();
        String responseText = new String(responseBytes, StandardCharsets.UTF_8);

        // Ollama /api/chat ou /api/generate peut renvoyer du "NDJSON" (stream) ou un JSON unique.
        // On cherche le dernier objet avec "done": true.
        try {
            JsonNode lastDone = null;

            for (String line : responseText.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;

                JsonNode node = mapper.readTree(line);
                if (node.path("done").asBoolean(false)) {
                    lastDone = node;
                }
            }

            // si non-stream: un seul JSON
            if (lastDone == null && responseText.trim().startsWith("{")) {
                JsonNode node = mapper.readTree(responseText);
                if (node.path("done").asBoolean(false)) {
                    lastDone = node;
                }
            }

            if (lastDone != null) {
                int prompt = lastDone.path("prompt_eval_count").asInt(-1);
                int completion = lastDone.path("eval_count").asInt(-1);

                if (prompt >= 0 && completion >= 0) {
                    int total = prompt + completion;

                    // "restants" côté contexte (pratique)
                    int remainingPrompt = numCtx - prompt;
                    int remainingTotal = numCtx - total;

                    log.info("OLLAMA TOKENS [{} {}] prompt={}, completion={}, total={}, remaining(prompt)={}, remaining(total)={}",
                        request.getMethod(), request.getURI().getPath(),
                        prompt, completion, total, remainingPrompt, remainingTotal);
                }
            }
        } catch (Exception e) {
            // debug only, ne casse jamais l'appel
            log.debug("Failed to parse Ollama token metrics from response", e);
        }

        // On renvoie une réponse "reconstituée" avec le body intact pour que Spring AI puisse lire normalement.
        return new CachedBodyClientHttpResponse(response, responseBytes);
    }

    /**
     * Wrapper qui remet le body en mémoire après lecture.
     */
    private static final class CachedBodyClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] body;

        private CachedBodyClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override public org.springframework.http.HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public int getRawStatusCode() throws IOException { return delegate.getRawStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public void close() { delegate.close(); }
        @Override public org.springframework.http.HttpHeaders getHeaders() { return delegate.getHeaders(); }

        @Override
        public java.io.InputStream getBody() {
            return new ByteArrayInputStream(body);
        }
    }
}
