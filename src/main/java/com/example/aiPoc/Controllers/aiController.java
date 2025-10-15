package com.example.aiPoc.Controllers;

import com.example.aiPoc.Services.AIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class aiController {
    private final AIService aiService;
    public aiController(AIService aiService) {
        this.aiService = aiService;
    }
    @GetMapping
    public String chat(@RequestParam String prompt) {
        return aiService.chat(prompt);
    }
}
