package org.example.cavavin.dal;

import org.example.cavavin.bo.Couleur;

// Interface de projection de Spring Data. Elle n'a pas besoin d'être implémentée.
public interface BouteilleResume {

    // Retourne le nom du champ "nom" du document Bouteille
    String getNom();

    // Retourne le champ "millesime"
    Integer getMillesime();

    // Spring Data gère l'imbrication dans le sous-document Couleur
    Couleur getCouleur();

    // On ignore le champ Region et Avis dans cette vue optimisée.
}