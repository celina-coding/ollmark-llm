package com.example.aiPoc.services.ai;

import com.example.aiPoc.models.*;
import com.example.aiPoc.repositories.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service chargé de détecter des mots-clés liés à Ollca dans un prompt utilisateur,
 * d’interroger la base de données afin de récupérer les produits et boutiques associés,
 * puis de formater ces informations dans un bloc de contexte exploitable par un moteur d’IA.
 */
@Service
public class OllcaEntityExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(OllcaEntityExtractorService.class);

    /**
     * Static list of keywords related to Ollca entities.
     * Used for simple pattern detection in user prompts.
     */
    private static final List<String> OLLCA_KEYWORDS = List.of(
        "boeuf", "saucisses", "camembert", "comté",
        "boucherie", "fromagerie", "versailles", "paris"
    );

    private final ProduitRepository produitRepository;
    private final BoutiqueRepository boutiqueRepository;

    /**
     * Constructs the service with required product and shop repositories.
     *
     * @param produitRepository  repository used to query {@link Produit} entities
     * @param boutiqueRepository repository used to query {@link Boutique} entities
     */
    public OllcaEntityExtractorService(
        ProduitRepository produitRepository,
        BoutiqueRepository boutiqueRepository
    ) {
        this.produitRepository = produitRepository;
        this.boutiqueRepository = boutiqueRepository;
    }

    /**
     * Detects whether the user prompt contains any Ollca-related keyword,
     * fetches matching products and shops, and formats them into a readable
     * context block to be injected into an AI prompt.
     *
     * @param userPrompt raw user query
     * @return formatted context with matched products and shops, or an empty string if no keyword is found
     */
    public String extractAndFormatData(String userPrompt) {
        String promptLower = userPrompt.toLowerCase(Locale.FRENCH);

        String foundKeyword = OLLCA_KEYWORDS.stream()
            .filter(promptLower::contains)
            .findFirst()
            .orElse(null);

        if (foundKeyword == null) return "";

        logger.info("Mot-clé Ollca détecté: {}", foundKeyword);

        StringBuilder dataContext = new StringBuilder();

        List<Produit> produits = produitRepository.findByNomContainingIgnoreCase(foundKeyword);
        if (!produits.isEmpty()) {
            dataContext.append(formatProduitContext(produits, foundKeyword));
        }

        List<Boutique> boutiques = boutiqueRepository.findByNomContainingIgnoreCase(foundKeyword);
        if (boutiques.isEmpty()) {
            boutiques = boutiqueRepository.findByTypeContainingIgnoreCase(foundKeyword);
        }
        if (boutiques.isEmpty()) {
            boutiques = boutiqueRepository.findByVilleContainingIgnoreCase(foundKeyword);
        }

        if (!boutiques.isEmpty()) {
            dataContext.append(formatBoutiqueContext(boutiques));
        }

        if (dataContext.length() > 0) {
            dataContext.insert(0, "\n[CONTEXTE BDD OLLCA - UTILISEZ CES DONNÉES SI PERTINENT]\n");
            dataContext.append("[FIN CONTEXTE OLLCA]\n");
            return dataContext.toString();
        }

        return "";
    }

    /**
     * Formats a list of products into a textual contextual block.
     *
     * @param produits list of matching products
     * @param keyword  keyword that triggered the search
     * @return formatted string describing the products
     */
    private String formatProduitContext(List<Produit> produits, String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append("Produits trouvés contenant \"").append(keyword).append("\" :\n");

        produits.stream().limit(5).forEach(p -> {
            sb.append("- Produit: **").append(p.getNom()).append("**")
              .append(" | Prix: ").append(p.getPrix()).append("€")
              .append(" | Origine: ").append(p.getOrigine())
              .append(" | Boutique: ").append(p.getBoutique().getNom()).append("\n");
        });

        return sb.toString();
    }

    /**
     * Formats a list of shops into a textual contextual block.
     *
     * @param boutiques list of matching shops
     * @return formatted string describing the shops
     */
    private String formatBoutiqueContext(List<Boutique> boutiques) {
        StringBuilder sb = new StringBuilder();
        sb.append("Boutiques trouvées :\n");

        boutiques.stream().limit(2).forEach(b -> {
            sb.append("- Boutique: **").append(b.getNom()).append("**")
              .append(" | Type: ").append(b.getType())
              .append(" | Ville: ").append(b.getVille())
              .append(" | Tél: ").append(b.getTelephone() != null ? b.getTelephone() : "N/A").append("\n");
        });

        return sb.toString();
    }
}