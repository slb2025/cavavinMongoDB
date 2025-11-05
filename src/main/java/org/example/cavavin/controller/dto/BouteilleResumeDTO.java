package org.example.cavavin.controller.dto;

import org.example.cavavin.bo.BouteilleResume;
import org.example.cavavin.bo.Couleur;

// DTO basé sur une classe pour garantir une sérialisation JSON fiable
public class BouteilleResumeDTO {

    private final String nom;
    private final Integer millesime;
    private final Couleur couleur;

    /**
     * Constructeur pour mapper les données de l'interface de projection.
     */
    public BouteilleResumeDTO(BouteilleResume resume) {
        this.nom = resume.getNom();
        this.millesime = resume.getMillesime();
        this.couleur = resume.getCouleur();
    }

    // --- Getters (Nécessaires pour la sérialisation par Jackson) ---

    public String getNom() {
        return nom;
    }

    public Integer getMillesime() {
        return millesime;
    }

    public Couleur getCouleur() {
        return couleur;
    }
}