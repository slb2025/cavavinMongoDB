package org.example.cavavin.service;

import org.example.cavavin.bo.Avis;
import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.BouteilleResume;
import org.example.cavavin.controller.dto.BouteilleResumeDTO;
import org.example.cavavin.dal.AvisRepository;
import org.example.cavavin.dal.BouteilleRepository;
import org.example.cavavin.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Cette annotation marque la classe comme un bean de service Spring
@Service
public class BouteilleServiceImpl implements BouteilleService {

    private final BouteilleRepository bouteilleRepository;
    private final AvisRepository avisRepository;

    @Autowired
    public BouteilleServiceImpl(BouteilleRepository bouteilleRepository, AvisRepository avisRepository) {
        this.bouteilleRepository = bouteilleRepository;
        this.avisRepository = avisRepository;
    }

    // --- Opération 1 : Ajout Atomique d'Avis ---
    @Override
    @Transactional // CLÉ : Assure que les deux écritures (Avis et Bouteille) sont atomiques
    public Avis ajouterAvis(String bouteilleId, String commentaire) throws ResourceNotFoundException {
        // 1. Trouver la Bouteille ou lever une exception
        Bouteille bouteille = bouteilleRepository.findById(bouteilleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bouteille", bouteilleId));

        // 2. Créer le nouvel Avis et y injecter la référence de la Bouteille
        Avis nouvelAvis = new Avis(commentaire, bouteille);

        // 3. Sauvegarder l'Avis (première écriture)
        nouvelAvis = avisRepository.save(nouvelAvis);

        // 4. Mettre à jour la Bouteille pour y ajouter l'ID du nouvel Avis
        // Spring Data gère automatiquement la liste d'IDs grâce à @DocumentReference
        // Si la liste est null, on l'initialise.
        List<Avis> avisList = bouteille.getAvis();
        if (avisList == null || avisList.isEmpty()) {
            bouteille.setAvis(List.of(nouvelAvis));
        } else {
            avisList.add(nouvelAvis);
            bouteille.setAvis(avisList);
        }

        // 5. Sauvegarder la Bouteille (deuxième écriture)
        bouteilleRepository.save(bouteille);

        // Si une erreur survient entre l'étape 3 et 5, la transaction annule l'étape 3 (Rollback).
        return nouvelAvis;
    }

    // --- Opération 2 : Suppression en Cascade ---
    @Override
    @Transactional // CLÉ : Assure que la suppression de l'Avis et de la Bouteille sont atomiques
    public void supprimerBouteilleEtAvisAssocies(String bouteilleId) throws ResourceNotFoundException {
        // 1. Trouver la Bouteille ou lever une exception
        Bouteille bouteille = bouteilleRepository.findById(bouteilleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bouteille", bouteilleId));

        // 2. Suppression de tous les Avis liés (première écriture)
        // Utilise la méthode deleteByBouteille du Repository pour une suppression efficace
        avisRepository.deleteByBouteille(bouteille);

        // 3. Suppression de la Bouteille (deuxième écriture)
        bouteilleRepository.delete(bouteille);

        // Si la suppression de la bouteille échoue, la suppression des avis est annulée.
    }

    // --- Opération 3 : Lecture Optimisée (Simple délégation pour l'Eager Loading) ---
    @Override
    public List<Bouteille> findAllWithRegionEagerly() {
        // Délégation simple de la méthode optimisée de la DAL à la couche Service
        return bouteilleRepository.findAllWithRegionEagerly();
    }

    // --- Opération 4 : Consultation détaillée ---
    @Override
    public Bouteille findById(String id) throws ResourceNotFoundException {
        // Simple délégation au Repository, gère l'exception 404 via l'orElseThrow
        return bouteilleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bouteille", id));
    }

    // --- Opération 5 : Projection de Performance (Mapping vers DTO) ---
    @Override
    public List<BouteilleResumeDTO> findAllResume() {
        // Délégation au Repository pour obtenir la projection (interface)
        List<BouteilleResume> resumes = bouteilleRepository.findAllBy(); // Suppose que BouteilleRepository a findAllBy()

        // Conversion de l'interface de projection Spring Data vers le DTO de classe concrète pour l'API REST
        return resumes.stream()
                .map(BouteilleResumeDTO::new)
                .collect(Collectors.toList());
    }
}