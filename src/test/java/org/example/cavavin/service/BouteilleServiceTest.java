package org.example.cavavin.service;

import org.example.cavavin.bo.Avis;
import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.Region;
import org.example.cavavin.dal.AvisRepository;
import org.example.cavavin.dal.BouteilleRepository;
import org.example.cavavin.dal.RegionRepository;
import org.example.cavavin.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BouteilleServiceTest {

    // Le Service à tester
    @Autowired
    private BouteilleService bouteilleService;

    // Les Repositories pour l'état initial et la vérification post-test
    @Autowired
    private BouteilleRepository bouteilleRepository;
    @Autowired
    private AvisRepository avisRepository;
    @Autowired
    private RegionRepository regionRepository;

    private Bouteille testBouteille;
    private Region testRegion;

    @BeforeEach
    void setup() {
        // Assurer un état de base propre pour chaque test
        bouteilleRepository.deleteAll();
        avisRepository.deleteAll();
        regionRepository.deleteAll();

        // Créer une région de référence
        testRegion = regionRepository.save(new Region("Bourgogne"));

        // Créer une bouteille de base pour les tests
        testBouteille = new Bouteille("Chassagne-Montrachet", 2018);
        testBouteille.setRegion(testRegion);
        testBouteille = bouteilleRepository.save(testBouteille);
    }

    // --- Test de l'Opération 1 : Ajout Atomique d'un Avis ---

    @Test
    @DisplayName("A. Ajout d'Avis OK : L'avis et la bouteille sont mis à jour")
    void testAjouterAvis_casOk() {
        // Arrange
        String commentaire = "Un vin d'exception.";
        long initialAvisCount = avisRepository.count();

        // Act
        Avis nouvelAvis = bouteilleService.ajouterAvis(testBouteille.getId(), commentaire);

        // Assert
        // 1. L'Avis a été créé
        assertNotNull(nouvelAvis.getId(), "L'ID de l'avis doit être généré.");
        assertEquals(initialAvisCount + 1, avisRepository.count(), "Un avis doit avoir été ajouté à la collection 'avis'.");

        // 2. La Bouteille a été mise à jour avec la référence
        Bouteille bouteilleApres = bouteilleRepository.findById(testBouteille.getId()).orElseThrow();
        assertFalse(bouteilleApres.getAvis().isEmpty(), "La liste d'avis de la Bouteille ne doit pas être vide.");
        assertEquals(1, bouteilleApres.getAvis().size());
    }

    @Test
    @Transactional // Nécessaire pour simuler le comportement transactionnel même en test d'échec
    @DisplayName("B. Ajout d'Avis Échec : Rollback en cas d'erreur de la bouteille parente")
    void testAjouterAvis_casEchecBouteilleNonTrouvee_rollback() {
        // Arrange
        String idInexistant = "ID_INEXISTANT";
        long initialAvisCount = avisRepository.count();

        // Act & Assert
        // On vérifie que la méthode lève l'exception métier
        assertThrows(ResourceNotFoundException.class, () -> {
            bouteilleService.ajouterAvis(idInexistant, "Ce test doit échouer.");
        }, "L'exception ResourceNotFoundException doit être levée.");

        // Vérification cruciale de l'Atomicité (Rollback) :
        // Le nombre d'avis ne doit pas avoir changé, même si la création de l'avis
        // a pu être tentée avant l'échec de la recherche de la bouteille.
        // Grâce à @Transactional, toutes les écritures sont annulées.
        assertEquals(initialAvisCount, avisRepository.count(),
                "Le nombre d'avis ne doit pas changer (Rollback réussi).");
    }

    // --- Test de l'Opération 2 : Suppression en Cascade ---

    @Test
    @DisplayName("C. Suppression Cascade OK : Bouteille et Avis sont supprimés")
    void testSupprimerBouteilleEtAvisAssocies_casOk() {
        // Arrange
        // Ajouter deux avis qui seront liés à la bouteille de test
        Avis avis1 = avisRepository.save(new Avis("Très bon vin.", testBouteille));
        Avis avis2 = avisRepository.save(new Avis("Commentaire moyen.", testBouteille));

        // Mettre à jour la liste des avis dans la bouteille (pour la cohérence)
        testBouteille.setAvis(List.of(avis1, avis2));
        bouteilleRepository.save(testBouteille);

        // Act
        bouteilleService.supprimerBouteilleEtAvisAssocies(testBouteille.getId());

        // Assert
        // 1. La Bouteille a disparu
        assertFalse(bouteilleRepository.findById(testBouteille.getId()).isPresent(),
                "La bouteille doit avoir été supprimée.");

        // 2. Les Avis associés ont disparu (Cascade)
        assertEquals(0, avisRepository.findByBouteille(testBouteille).size(),
                "Les avis associés doivent avoir été supprimés par la cascade du service.");
    }

    @Test
    @DisplayName("D. Suppression Cascade Échec : Bouteille non trouvée")
    void testSupprimerBouteilleEtAvisAssocies_casEchecBouteilleNonTrouvee() {
        // Arrange
        String idInexistant = "ID_NON_EXISTANT";

        // Act & Assert
        // L'exception doit être levée
        assertThrows(ResourceNotFoundException.class, () -> {
            bouteilleService.supprimerBouteilleEtAvisAssocies(idInexistant);
        }, "L'exception ResourceNotFoundException doit être levée pour un ID inexistant.");

        // S'assurer que les données initiales n'ont pas été affectées (au cas où)
        assertTrue(bouteilleRepository.findById(testBouteille.getId()).isPresent());
    }
}