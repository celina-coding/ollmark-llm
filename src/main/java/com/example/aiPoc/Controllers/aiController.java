package com.example.aiPoc.Controllers;

import com.example.aiPoc.DTO.ChatRequest;
import com.example.aiPoc.Services.AIService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:61873")
@RestController
@RequestMapping("/api/chat")
public class aiController {
    private final AIService aiService;
    public aiController(AIService aiService) {
        this.aiService = aiService;
    }
    @PostMapping
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        // Récupération du prompt
        String prompt = request.getPrompt();

        // Appel à ton service
        String response = aiService.chat(prompt);

        // Nettoyage du texte entre <think> ... </think>
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Retour sous forme JSON
        return Map.of("response", response);
    }
}
