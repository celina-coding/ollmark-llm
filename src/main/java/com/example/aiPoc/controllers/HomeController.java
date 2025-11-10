package com.example.aiPoc.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur responsable des redirections vers les pages principales de l'application.
 *
 * <p>Ce contrôleur permet de gérer la navigation vers les différentes pages frontales
 * accessibles via le navigateur (ex : page d'accueil, page de chat, page de test, etc.).</p>
 */
@Controller
public class HomeController {

    /**
     * Redirige vers la page d'accueil principale de l'application.
     *
     * @return une redirection HTTP vers la page {@code index.html}
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    /**
     * Redirige vers la page du module de chat.
     *
     * @return une redirection HTTP vers la page {@code chat.html}
     */
    @GetMapping("/chat")
    public String chat() {
        return "redirect:/chat.html";
    }

    /**
     * Redirige vers la page de test Penpot permettant d’expérimenter la génération
     * de code ou d’effectuer des tests liés aux fonctionnalités Penpot.
     *
     * @return une redirection HTTP vers la page {@code test-penpot.html}
     */
    @GetMapping("/test")
    public String testPenpot() {
        return "redirect:/test-penpot.html";
    }

    /**
     * Redirige vers la page d'évaluation, utilisée pour analyser, tester ou
     * évaluer les fonctionnalités de génération et les résultats produits.
     *
     * @return une redirection HTTP vers la page {@code evaluation.html}
     */
    @GetMapping("/evaluation")
    public String evaluation() {
        return "redirect:/evaluation.html";
    }
}