// src/main/java/com/example/aiPoc/repositories/ProduitRepository.java
package com.example.aiPoc.repositories;

import com.example.aiPoc.models.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Produit> findByNomContainingIgnoreCase(@Param("nom") String nom);
    
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nom) = LOWER(:nom)")
    List<Produit> findByNomExact(@Param("nom") String nom);
    
    List<Produit> findByOrigineContainingIgnoreCase(String origine);
}