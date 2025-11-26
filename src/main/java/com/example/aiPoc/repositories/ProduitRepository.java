package com.example.aiPoc.repositories;

import com.example.aiPoc.models.Produit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dépôt JPA dédié aux opérations de persistance et de recherche
 * concernant les entités {@link Produit}.
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    /**
     * Recherche les produits dont le nom contient partiellement la valeur
     * spécifiée, sans tenir compte de la casse.
     *
     * @param nom fragment du nom à rechercher
     * @return liste de produits correspondant à la recherche approximative
     */
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Produit> findByNomContainingIgnoreCase(@Param("nom") String nom);

    /**
     * Recherche les produits correspondant exactement au nom indiqué,
     * sans tenir compte de la casse.
     *
     * @param nom nom exact à rechercher
     * @return liste de produits correspondant strictement au nom indiqué
     */
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nom) = LOWER(:nom)")
    List<Produit> findByNomExact(@Param("nom") String nom);

    /**
     * Recherche les produits dont l'origine contient partiellement la valeur
     * spécifiée, en ignorant la casse. Cette méthode exploite la convention
     * de nommage Spring Data.
     *
     * @param origine fragment de l'origine à rechercher
     * @return liste de produits correspondant à la recherche sur l’origine
     */
    List<Produit> findByOrigineContainingIgnoreCase(String origine);
}