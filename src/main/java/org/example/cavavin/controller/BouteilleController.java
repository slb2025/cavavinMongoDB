package org.example.cavavin.controller;

import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.Avis;
import org.example.cavavin.controller.dto.BouteilleResumeDTO;
import org.example.cavavin.service.BouteilleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bouteilles")
// Pour la gestion des exceptions centralisée
// (Nous devrons implémenter un @ControllerAdvice plus tard)
public class BouteilleController {

    @Autowired
    private BouteilleService bouteilleService;

    // --- 1. ENDPOINTS DE LECTURE (GET) ---

    /**
     * Endpoint pour récupérer la liste complète des bouteilles.
     * Utilise la méthode optimisée (avec $lookup) pour le chargement Eager de la Région.
     * Les avis restent en Lazy Loading.
     * Ex: GET /api/bouteilles
     */
    @GetMapping
    public List<Bouteille> findAll() {
        // Délègue au service la méthode qui utilise le $lookup optimisé
        return bouteilleService.findAllWithRegionEagerly();
    }

    /**
     * Endpoint pour la consultation détaillée d'une bouteille par ID.
     * Ex: GET /api/bouteilles/60c72b9f36f9011e4c34a36f
     * (Les avis et la région seront chargés selon leur stratégie : Eager/Lazy)
     */
    @GetMapping("/{id}")
    public Bouteille findById(@PathVariable String id) {
        // Le service gère la levée de l'exception 404 si la ressource n'est pas trouvée
        return bouteilleService.findById(id);
    }

    // --- 2. ENDPOINTS D'ÉCRITURE/MODIFICATION (POST/DELETE) ---

    /**
     * Endpoint pour ajouter un nouvel avis à une bouteille existante (Opération Atomique).
     * Ex: POST /api/bouteilles/60c72b9f36f9011e4c34a36f/avis
     * Corps de la requête (Body): { "commentaire": "Magnifique vin de garde." }
     */
    @PostMapping("/{bouteilleId}/avis")
    @ResponseStatus(HttpStatus.CREATED) // Retourne 201 Created si succès
    public Avis ajouterAvis(@PathVariable String bouteilleId, @RequestBody Avis newAvis) {
        // Le service gère l'écriture dans les deux collections (Avis + Bouteille) de manière atomique
        return bouteilleService.ajouterAvis(bouteilleId, newAvis.getCommentaire());
    }

    /**
     * Endpoint pour la suppression en cascade d'une bouteille et de tous ses avis.
     * Ex: DELETE /api/bouteilles/60c72b9f36f9011e4c34a36f
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Retourne 204 No Content si succès
    public void deleteBouteille(@PathVariable String id) {
        // Le service gère la logique de suppression en cascade
        bouteilleService.supprimerBouteilleEtAvisAssocies(id);
    }

    // --- 3. ENDPOINT DE PERFORMANCE (Projection) ---

    /**
     * Endpoint pour charger uniquement les résumés des bouteilles (Projection).
     * Ex: GET /api/bouteilles/resume
     */
    @GetMapping("/resume")
    public List<BouteilleResumeDTO> findAllResume() {
        return bouteilleService.findAllResume();
    }
}