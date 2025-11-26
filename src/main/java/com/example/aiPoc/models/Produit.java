package com.example.aiPoc.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entité représentant un produit commercialisé par une boutique.
 * Un produit contient des informations essentielles telles que son nom,
 * son origine, sa quantité, son prix ainsi que la boutique associée.
 */
@Entity
@Table(name = "produit")
public class Produit {

    /**
     * Identifiant unique du produit.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Integer idProduit;

    /**
     * Boutique à laquelle appartient le produit.
     * Plusieurs produits peuvent être associés à une même boutique.
     */
    @ManyToOne
    @JoinColumn(name = "id_boutique", nullable = false)
    private Boutique boutique;

    /**
     * Nom du produit.
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Origine géographique ou descriptive du produit.
     */
    @Column(name = "origine", length = 100)
    private String origine;

    /**
     * Quantité ou format du produit (ex. 1kg, 500g, 6 pièces…).
     */
    @Column(name = "quantite", length = 100)
    private String quantite;

    /**
     * Prix du produit exprimé en BigDecimal pour garantir la précision.
     */
    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    /**
     * Constructeur par défaut utilisé par JPA.
     */
    public Produit() {}

    /** @return identifiant unique du produit */
    public Integer getIdProduit() {
        return idProduit;
    }

    /** @return boutique associée au produit */
    public Boutique getBoutique() {
        return boutique;
    }

    /** @return nom du produit */
    public String getNom() {
        return nom;
    }

    /** @return origine du produit */
    public String getOrigine() {
        return origine;
    }

    /** @return quantité du produit */
    public String getQuantite() {
        return quantite;
    }

    /** @return prix du produit */
    public BigDecimal getPrix() {
        return prix;
    }

    /**
     * Définit l'identifiant unique du produit.
     * @param idProduit identifiant à définir
     */
    public void setIdProduit(Integer idProduit) {
        this.idProduit = idProduit;
    }

    /**
     * Associe ce produit à une boutique.
     * @param boutique boutique propriétaire du produit
     */
    public void setBoutique(Boutique boutique) {
        this.boutique = boutique;
    }

    /**
     * Définit le nom du produit.
     * @param nom nouveau nom
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * Définit l'origine du produit.
     * @param origine nouvelle origine
     */
    public void setOrigine(String origine) {
        this.origine = origine;
    }

    /**
     * Définit la quantité ou le format du produit.
     * @param quantite nouvelle quantité
     */
    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }

    /**
     * Définit le prix du produit.
     * @param prix nouveau prix
     */
    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }
}