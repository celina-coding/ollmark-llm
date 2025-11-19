package com.example.aiPoc.models;

import jakarta.persistence.*;

@Entity
@Table(name = "boutique")
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boutique")
    private Integer idBoutique;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "adresse", nullable = false, length = 255)
    private String adresse; 
    @Column(name = "code_postal", nullable = false, length = 20)
    private String codePostal; 

    @Column(name = "ville", nullable = false, length = 100)
    private String ville; 

    @Column(name = "pays", nullable = false, length = 100)
    private String pays;   

    @Column(name = "telephone", length = 20) 
    private String telephone;   

    @Column(name = "email", length = 100) 
    private String email;

    @Column(name = "site_web", length = 100)
    private String siteWeb;   

    @Column(name = "horaires_ouverture", length = 255)
    private String horairesOuverture;   

    @Column(name = "retrait")
    private Boolean retrait;   

    @Column(name = "livraison")
    private Boolean livraison;   

    public Boutique() {}

    public Integer getIdBoutique() {
        return idBoutique;
    }

    public String getNom() {
        return nom;
    }

    public String getType() {
        return type;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public String getVille() {
        return ville;
    }

    public String getPays() {
        return pays;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getEmail() {
        return email;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public String getHorairesOuverture() {
        return horairesOuverture;
    }

    public Boolean getRetrait() {
        return retrait;
    }

    public Boolean getLivraison() {
        return livraison;
    }

    public void setIdBoutique(Integer idBoutique) {
        this.idBoutique = idBoutique;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public void setHorairesOuverture(String horairesOuverture) {
        this.horairesOuverture = horairesOuverture;
    }

    public void setRetrait(Boolean retrait) {
        this.retrait = retrait;
    }

    public void setLivraison(Boolean livraison) {
        this.livraison = livraison;
    }
}