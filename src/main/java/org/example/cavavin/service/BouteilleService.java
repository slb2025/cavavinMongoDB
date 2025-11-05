package org.example.cavavin.service;

import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.Avis;
import org.example.cavavin.exception.ResourceNotFoundException;

import java.util.List;

public interface BouteilleService {

    /**
     * Ajoute un Avis à une Bouteille existante de manière atomique (opération transactionnelle).
     * @param bouteilleId L'ID de la bouteille à mettre à jour.
     * @param commentaire Le contenu du nouvel avis.
     * @return Le nouvel objet Avis créé.
     * @throws ResourceNotFoundException si la Bouteille n'existe pas.
     */
    Avis ajouterAvis(String bouteilleId, String commentaire) throws ResourceNotFoundException;

    /**
     * Supprime une Bouteille et tous les Avis qui lui sont associés (suppression en cascade).
     * @param bouteilleId L'ID de la bouteille à supprimer.
     * @throws ResourceNotFoundException si la Bouteille n'existe pas.
     */
    void supprimerBouteilleEtAvisAssocies(String bouteilleId) throws ResourceNotFoundException;

    /**
     * Récupère toutes les bouteilles avec les régions chargées en mode Eager.
     */
    List<Bouteille> findAllWithRegionEagerly();
}