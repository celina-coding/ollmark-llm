package com.example.aiPoc.models;

import jakarta.persistence.*;

/**
 * Entité représentant une boutique enregistrée dans la base de données.
 * Une boutique contient des informations d’identification, de localisation,
 * de contact ainsi que des détails sur les services proposés
 * (retrait, livraison, horaires, etc.).
 */
@Entity
@Table(name = "boutique")
public class Boutique {

    /**
     * Identifiant unique de la boutique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boutique")
    private Integer idBoutique;

    /**
     * Nom de la boutique.
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Type ou catégorie de la boutique
     * (ex. boucherie, fromagerie, primeur...).
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /**
     * Adresse complète de la boutique.
     */
    @Column(name = "adresse", nullable = false, length = 255)
    private String adresse;

    /**
     * Code postal associé à la boutique.
     */
    @Column(name = "code_postal", nullable = false, length = 20)
    private String codePostal;

    /**
     * Ville de localisation de la boutique.
     */
    @Column(name = "ville", nullable = false, length = 100)
    private String ville;

    /**
     * Pays où se situe la boutique.
     */
    @Column(name = "pays", nullable = false, length = 100)
    private String pays;

    /**
     * Numéro de téléphone de contact de la boutique.
     */
    @Column(name = "telephone", length = 20)
    private String telephone;

    /**
     * Adresse email de contact de la boutique.
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Site web officiel de la boutique.
     */
    @Column(name = "site_web", length = 100)
    private String siteWeb;

    /**
     * Texte décrivant les horaires d’ouverture de la boutique.
     */
    @Column(name = "horaires_ouverture", length = 255)
    private String horairesOuverture;

    /**
     * Indique si la boutique propose un service de retrait sur place.
     */
    @Column(name = "retrait")
    private Boolean retrait;

    /**
     * Indique si la boutique propose un service de livraison.
     */
    @Column(name = "livraison")
    private Boolean livraison;

    /**
     * Constructeur par défaut requis par JPA.
     */
    public Boutique() {}

    /** @return identifiant unique de la boutique */
    public Integer getIdBoutique() {
        return idBoutique;
    }

    /** @return nom de la boutique */
    public String getNom() {
        return nom;
    }

    /** @return type/catégorie de la boutique */
    public String getType() {
        return type;
    }

    /** @return adresse complète de la boutique */
    public String getAdresse() {
        return adresse;
    }

    /** @return code postal de la boutique */
    public String getCodePostal() {
        return codePostal;
    }

    /** @return ville dans laquelle se situe la boutique */
    public String getVille() {
        return ville;
    }

    /** @return pays où se trouve la boutique */
    public String getPays() {
        return pays;
    }

    /** @return numéro de téléphone de la boutique */
    public String getTelephone() {
        return telephone;
    }

    /** @return adresse email de la boutique */
    public String getEmail() {
        return email;
    }

    /** @return site web de la boutique */
    public String getSiteWeb() {
        return siteWeb;
    }

    /** @return horaires d’ouverture de la boutique */
    public String getHorairesOuverture() {
        return horairesOuverture;
    }

    /** @return true si la boutique propose le retrait, sinon false */
    public Boolean getRetrait() {
        return retrait;
    }

    /** @return true si la boutique propose la livraison, sinon false */
    public Boolean getLivraison() {
        return livraison;
    }

    /**
     * Définit l’identifiant unique de la boutique.
     * @param idBoutique identifiant à définir
     */
    public void setIdBoutique(Integer idBoutique) {
        this.idBoutique = idBoutique;
    }

    /**
     * Définit le nom de la boutique.
     * @param nom nouveau nom
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * Définit le type de boutique.
     * @param type nouvelle catégorie
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Définit l’adresse complète de la boutique.
     * @param adresse nouvelle adresse
     */
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    /**
     * Définit le code postal de la boutique.
     * @param codePostal nouveau code postal
     */
    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    /**
     * Définit la ville de la boutique.
     * @param ville nouvelle ville
     */
    public void setVille(String ville) {
        this.ville = ville;
    }

    /**
     * Définit le pays de la boutique.
     * @param pays nouveau pays
     */
    public void setPays(String pays) {
        this.pays = pays;
    }

    /**
     * Définit le numéro de téléphone de la boutique.
     * @param telephone nouveau numéro
     */
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    /**
     * Définit l’adresse email de la boutique.
     * @param email nouvelle adresse email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Définit le site web de la boutique.
     * @param siteWeb nouvelle URL du site
     */
    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    /**
     * Définit les horaires d’ouverture.
     * @param horairesOuverture texte décrivant les horaires
     */
    public void setHorairesOuverture(String horairesOuverture) {
        this.horairesOuverture = horairesOuverture;
    }

    /**
     * Indique si la boutique propose le retrait.
     * @param retrait true si oui, false sinon
     */
    public void setRetrait(Boolean retrait) {
        this.retrait = retrait;
    }

    /**
     * Indique si la boutique propose la livraison.
     * @param livraison true si oui, false sinon
     */
    public void setLivraison(Boolean livraison) {
        this.livraison = livraison;
    }
}