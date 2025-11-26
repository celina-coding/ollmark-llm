package com.example.aiPoc.repositories;

import com.example.aiPoc.models.Boutique;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dépôt JPA dédié aux opérations de persistance et de recherche
 * concernant les entités {@link Boutique}.
 *
 * <p>Ce dépôt fournit des méthodes permettant d’effectuer des recherches
 * insensibles à la casse sur différents attributs tels que le nom, le type
 * ou la ville de la boutique.
 */
@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {

    /**
     * Recherche les boutiques dont le nom contient la valeur spécifiée,
     * sans tenir compte de la casse.
     *
     * @param nom fragment du nom à rechercher
     * @return liste de boutiques correspondant partiellement au nom indiqué
     */
    @Query("SELECT b FROM Boutique b WHERE LOWER(b.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Boutique> findByNomContainingIgnoreCase(@Param("nom") String nom);

    /**
     * Recherche les boutiques dont le type contient la valeur spécifiée,
     * sans tenir compte de la casse.
     *
     * @param type fragment du type à rechercher
     * @return liste de boutiques correspondant partiellement au type indiqué
     */
    @Query("SELECT b FROM Boutique b WHERE LOWER(b.type) LIKE LOWER(CONCAT('%', :type, '%'))")
    List<Boutique> findByTypeContainingIgnoreCase(@Param("type") String type);

    /**
     * Recherche les boutiques dont la ville contient la valeur spécifiée,
     * sans distinction de casse. Cette méthode exploite la convention Spring Data.
     *
     * @param ville fragment du nom de ville à rechercher
     * @return liste de boutiques correspondant partiellement à la ville indiquée
     */
    List<Boutique> findByVilleContainingIgnoreCase(String ville);
}