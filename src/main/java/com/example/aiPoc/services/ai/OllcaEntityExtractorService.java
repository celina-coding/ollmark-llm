package com.example.aiPoc.services.ai;

import com.example.aiPoc.models.Produit;
import com.example.aiPoc.models.Boutique;
import com.example.aiPoc.repositories.ProduitRepository;
import com.example.aiPoc.repositories.BoutiqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class OllcaEntityExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(OllcaEntityExtractorService.class);
    
    // là j'utlise une liste de mots-clés statiques pour détecter les requêtes Ollca mais cette solution peut être améliorée 
    // notamment en remplaçant la liste statique par une recherche Full-Text directement dans la base de données 
    // (PostgreSQL FTS avec TSVECTOR et index GIN) pour garantir la scalabilité et la performance, 
  
    private static final List<String> OLLCA_KEYWORDS = List.of(
        "boeuf", 
        "saucisses", 
        "camembert", 
        "comté", 
        "boucherie", 
        "fromagerie", 
        "versailles",
        "paris"
    );

    private final ProduitRepository produitRepository;
    private final BoutiqueRepository boutiqueRepository;

    public OllcaEntityExtractorService(ProduitRepository produitRepository, BoutiqueRepository boutiqueRepository) {
        this.produitRepository = produitRepository;
        this.boutiqueRepository = boutiqueRepository;
    }

    public String extractAndFormatData(String userPrompt) {
        String promptLower = userPrompt.toLowerCase(Locale.FRENCH);
        
        String foundKeyword = OLLCA_KEYWORDS.stream()
            .filter(promptLower::contains)
            .findFirst()
            .orElse(null);

        if (foundKeyword == null) {
            return ""; 
        }

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