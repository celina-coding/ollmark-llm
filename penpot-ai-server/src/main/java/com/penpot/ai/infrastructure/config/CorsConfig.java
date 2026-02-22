package com.penpot.ai.infrastructure.config;

import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(
                        "http://localhost:4400",
                        "http://127.0.0.1:4400"
                    )
                    .allowedMethods("GET", "POST", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(false);
            }
        };
    }
}