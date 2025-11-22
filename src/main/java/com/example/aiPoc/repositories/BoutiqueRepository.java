// src/main/java/com/example/aiPoc/repositories/BoutiqueRepository.java
package com.example.aiPoc.repositories;

import com.example.aiPoc.models.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    
    @Query("SELECT b FROM Boutique b WHERE LOWER(b.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Boutique> findByNomContainingIgnoreCase(@Param("nom") String nom);
    
    @Query("SELECT b FROM Boutique b WHERE LOWER(b.type) LIKE LOWER(CONCAT('%', :type, '%'))")
    List<Boutique> findByTypeContainingIgnoreCase(@Param("type") String type);
    
    List<Boutique> findByVilleContainingIgnoreCase(String ville);
}