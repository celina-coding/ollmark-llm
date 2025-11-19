package com.example.aiPoc.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produit")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Integer idProduit;

    @ManyToOne
    @JoinColumn(name = "id_boutique", nullable = false)
    private Boutique boutique;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "origine", length = 100)
    private String origine;

    @Column(name = "quantite", length = 100)
    private String quantite;

    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    public Produit() {}

    public Integer getIdProduit() {
        return idProduit;
    }

    public Boutique getBoutique() {
        return boutique;
    }

    public String getNom() {
        return nom;
    }

    public String getOrigine() {
        return origine;
    }

    public String getQuantite() {
        return quantite;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    // Setters
    public void setIdProduit(Integer idProduit) {
        this.idProduit = idProduit;
    }

    public void setBoutique(Boutique boutique) {
        this.boutique = boutique;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }
}