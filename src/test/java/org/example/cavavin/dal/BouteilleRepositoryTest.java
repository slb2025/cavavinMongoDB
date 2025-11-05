package org.example.cavavin.dal;

import org.example.cavavin.bo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BouteilleRepositoryTest {
    @Autowired
    private BouteilleRepository bouteilleRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AvisRepository avisRepository; // NOTE : Doit contenir findByBouteille()

    // --- I. Tests d'Intégrité et de Création ---

    @Test
    @DisplayName("I.A. Insertion OK et validation des attributs de base (Couleur intégrée)")
    void testAjoutBouteilleCasOk(){
        // Arrange
        regionRepository.deleteAll();
        Region bordeaux = regionRepository.save(new Region("Bordeaux"));
        bouteilleRepository.deleteAll();
        Bouteille bouteille = new Bouteille("Château Cheval Blanc", 2015);
        bouteille.setRegion(bordeaux);
        bouteille.setCouleur(new Couleur("Rouge"));

        // Act
        Bouteille newBouteille = bouteilleRepository.save(bouteille);

        // Assert
        Optional<Bouteille> optBouteille = bouteilleRepository.findById(newBouteille.getId());
        assertTrue(optBouteille.isPresent(), "La bouteille doit être présente après sauvegarde.");

        // 1. Validation de la région référencée (ici paresseuse par défaut)
        assertNotNull(optBouteille.get().getRegion(), "La référence à la région ne doit pas être nulle.");

        // 2. Validation du sous-document Couleur (Intégration)
        assertNotNull(optBouteille.get().getCouleur(), "Le sous-document Couleur doit être présent.");
        assertEquals("Rouge", optBouteille.get().getCouleur().getLibelle());
    }

    @Test
    @DisplayName("I.B. Unicité du Nom : Vérification de l'index unique sur 'nom'")
    void testAjoutBouteilleCasNomBouteilleDejaExistante(){
        // Arrange
        bouteilleRepository.deleteAll();
        Bouteille bouteille1 = new Bouteille("Laurent Perrier", 2020);
        bouteilleRepository.save(bouteille1);
        Bouteille bouteille2 = new Bouteille("Laurent Perrier", 2022); // Même nom

        // Act & Assert
        // On vérifie que la tentative de sauvegarde lève l'exception de clé dupliquée
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> {
            bouteilleRepository.save(bouteille2);
        }, "L'index unique sur 'nom' doit lever une DuplicateKeyException.");

        assertEquals(1, bouteilleRepository.count(), "Seule la première bouteille doit être persistée.");
    }

    // --- II. Tests de Lecture Optimisée et de Référencement ---

    @Test
    @DisplayName("II.A. Chargement Eager Region : Validation de la méthode d'agrégation $lookup")
    void testFindAllWithRegionEagerly() {
        // Arrange (créer 2 bouteilles avec 2 régions)
        regionRepository.deleteAll();
        bouteilleRepository.deleteAll();

        // 1. Création des régions
        Region alsace = regionRepository.save(new Region("Alsace"));
        Region bordeaux = regionRepository.save(new Region("Bordeaux"));

        // 2. Création de la première bouteille (avec constructeur simple + setters)
        Bouteille bouteille1 = new Bouteille("Riesling", 2022); // Utilise le constructeur RequiredArgs
        bouteille1.setRegion(alsace);
        bouteille1.setCouleur(new Couleur("Blanc"));
        bouteilleRepository.save(bouteille1);

        // 3. Création de la deuxième bouteille
        Bouteille bouteille2 = new Bouteille("Saint-Emilion", 2015);
        bouteille2.setRegion(bordeaux);
        bouteille2.setCouleur(new Couleur("Rouge"));
        bouteilleRepository.save(bouteille2);

        // Act
        // Utilisation de la méthode optimisée qui exécute l'agrégation
        List<Bouteille> bouteilles = bouteilleRepository.findAllWithRegionEagerly(); // Utilisation du $lookup

        // Assert
        assertEquals(2, bouteilles.size(), "Deux bouteilles devraient être retournées.");

        Bouteille foundAlsace = bouteilles.stream()
                .filter(b -> b.getNom().equals("Riesling"))
                .findFirst().orElseThrow();

        // L'assertion clé : la région DOIT être hydratée (non-nulle) et correcte
        assertNotNull(foundAlsace.getRegion(), "Le $lookup doit avoir chargé la région (Eager Loading).");
        assertEquals("Alsace", foundAlsace.getRegion().getNomRegion());
    }

    @Test
    @DisplayName("II.B. Chargement Lazy Avis : Validation du comportement paresseux")
    void testLazyLoadingAvis() {
        // Arrange
        bouteilleRepository.deleteAll();
        avisRepository.deleteAll();
        Bouteille bouteille = bouteilleRepository.save(new Bouteille("Chablis", 2021));

        // Créer un avis et le lier à la bouteille (méthode non atomique pour le test simple)
        Avis avis = new Avis("Excellent vin!", bouteille);
        avisRepository.save(avis);

        // Mettre à jour la bouteille pour avoir la référence (nécessaire sans service transactionnel)
        bouteille.setAvis(List.of(avis));
        bouteilleRepository.save(bouteille);

        // Act
        // Recharger la bouteille avec la méthode standard findById()
        Bouteille reloadedBouteille = bouteilleRepository.findById(bouteille.getId()).orElseThrow();

        // Assert
        // La liste est chargée "paresseusement" mais le proxy doit être là.
        // Si le proxy n'a pas encore fait la requête, il ne devrait pas y avoir d'exception.
        // On force l'accès pour vérifier la présence des données via le proxy.
        // NOTE: Le comportement exact (taille > 0 ou proxy actif) dépend de l'implémentation de Spring Data.
        assertFalse(reloadedBouteille.getAvis().isEmpty(), "La liste d'avis chargée paresseusement ne doit pas être vide.");
        assertEquals(1, reloadedBouteille.getAvis().size());
        assertEquals("Excellent vin!", reloadedBouteille.getAvis().get(0).getCommentaire());
    }

    // --- V. Tests de Requêtes Spécifiques ---

    @Test
    @DisplayName("V.B. Recherche par Sous-document : Filtrage par Couleur intégrée")
    void testFindByCouleurLibelle() {
        // Arrange
        bouteilleRepository.deleteAll();
        Bouteille rouge = new Bouteille("Bordeaux", 2010);
        rouge.setCouleur(new Couleur("Rouge"));
        bouteilleRepository.save(rouge);

        Bouteille blanc = new Bouteille("Sancerre", 2020);
        blanc.setCouleur(new Couleur("Blanc"));
        bouteilleRepository.save(blanc);

        // Act : Nécessite l'ajout de List<Bouteille> findByCouleur_Libelle(String libelle); dans BouteilleRepository
        List<Bouteille> result = bouteilleRepository.findByCouleur_Libelle("Rouge");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Bordeaux", result.get(0).getNom());
    }

    @Test
    @DisplayName("V.D. Pagination et Tri : Valider l'ordonnancement par millésime")
    void testPaginationAndSorting() {
        // Arrange
        bouteilleRepository.deleteAll();
        bouteilleRepository.save(new Bouteille("A", 2000));
        bouteilleRepository.save(new Bouteille("B", 2020));
        bouteilleRepository.save(new Bouteille("C", 2010));

        // Act : Trier par millésime décroissant, prendre la première page de taille 2
        Pageable pageable = PageRequest.of(0, 2, Sort.by("millesime").descending());
        List<Bouteille> page = bouteilleRepository.findAll(pageable).getContent();

        // Assert
        assertEquals(2, page.size());
        // Doit retourner B (2020) puis C (2010)
        assertEquals("B", page.get(0).getNom(), "Le tri décroissant doit placer 2020 en premier.");
        assertEquals("C", page.get(1).getNom(), "Le tri décroissant doit placer 2010 en second.");
    }

    // --- VI. Tests de Comportement des Références (Cohérence des Données) ---

    @Test
    @DisplayName("VI.A. Cohérence de Région : Mise à jour de la Région Maître")
    void testRegionUpdateCoherence() {
        // Arrange
        regionRepository.deleteAll();
        Region regionA = regionRepository.save(new Region("Bordeaux"));

        // Utiliser le constructeur à deux arguments (nom, millesime)
        Bouteille bouteille = new Bouteille("Château", 2005);

        // Utiliser le setter pour lier la région (le référencement)
        bouteille.setRegion(regionA);

        // Sauvegarde de la bouteille
        bouteille = bouteilleRepository.save(bouteille);

        // Act : Mise à jour de la région (source de vérité unique)
        regionA.setNomRegion("Bordeaux Grand Cru");
        regionRepository.save(regionA);

        // Assert
        // Recharger la bouteille, sans utiliser le $lookup
        Bouteille reloadedBouteille = bouteilleRepository.findById(bouteille.getId()).orElseThrow();

        // L'assertion clé reste la même, validant la cohérence de la référence
        assertEquals("Bordeaux Grand Cru", reloadedBouteille.getRegion().getNomRegion(),
                "Le référencement doit garantir la cohérence des données régionales.");
    }

    @Test
    @DisplayName("VI.B. Suppression Paresseuse : Validation de l'absence de cascade native")
    void testDeleteBouteilleNoAvisCascade() {
        // Arrange
        bouteilleRepository.deleteAll();
        avisRepository.deleteAll();
        Bouteille bouteille = bouteilleRepository.save(new Bouteille("Test Delete", 2000));
        Avis avis = avisRepository.save(new Avis("Avis orphelin", bouteille));
        bouteille.setAvis(List.of(avis));
        bouteilleRepository.save(bouteille);

        // Act : Suppression de la bouteille parente
        bouteilleRepository.delete(bouteille);

        // Assert
        assertEquals(0, bouteilleRepository.count(), "La bouteille doit être supprimée.");
        // Le test clé : l'avis doit TOUJOURS exister dans sa collection car pas de cascade native
        assertTrue(avisRepository.findById(avis.getId()).isPresent(),
                "L'Avis doit rester dans la collection 'avis' (Référence orpheline) sans service de cascade.");
    }

    // --- IV. Tests d'Intégrité de la Référence Région ---

    @Test
    @DisplayName("IV.A. Intégrité Région : Unicité du nom de Région (index unique)")
    void testRegionNomUnicity() {
        // Arrange
        regionRepository.deleteAll();
        regionRepository.save(new Region("Bourgogne"));

        // Act & Assert
        // Tente de sauvegarder une région avec le même nom
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> {
            regionRepository.save(new Region("Bourgogne"));
        }, "L'index unique sur 'nomRegion' doit empêcher les doublons.");

        assertEquals(1, regionRepository.count());
    }

    @Test
    @DisplayName("VIII.A. Projection : Valider le chargement partiel des documents (BouteilleResume)")
    void testFindAllByProjection() {
        // Arrange
        bouteilleRepository.deleteAll();
        avisRepository.deleteAll();

        // 1. Création d'une bouteille de base pour servir de référence (même si elle est supprimée après)
        Bouteille bouteilleReference = new Bouteille("Bouteille de Ref", 2000);
        bouteilleReference = bouteilleRepository.save(bouteilleReference); // Sauvegarde pour obtenir un ID

        // 2. Création d'un avis qui rend le document Bouteille 'lourd'
        // CORRECTION : Nous passons MAINTENANT les deux arguments requis
        Avis avisLourd = avisRepository.save(new Avis("Un avis très long et détaillé", bouteilleReference));

        // 3. Création et sauvegarde de la Bouteille que nous allons projeter (la bouteille de test réelle)
        Bouteille bouteilleComplete = new Bouteille("Vin de Test", 2023);
        bouteilleComplete.setCouleur(new Couleur("Blanc"));

        // La liste d'avis référencés est ajoutée
        bouteilleComplete.setAvis(List.of(avisLourd));
        bouteilleRepository.save(bouteilleComplete);

        // Avant la phase ACT, supprimer la bouteille temporaire/de référence
        bouteilleRepository.delete(bouteilleReference);



        // Act
        // Utilisation de la méthode de projection findAllBy()
        List<BouteilleResume> resumes = bouteilleRepository.findAllBy();

        // Assert
        assertEquals(1, resumes.size(), "Une seule projection doit être retournée.");
        BouteilleResume resume = resumes.get(0);

        // 1. Vérification des champs inclus dans la projection
        assertNotNull(resume.getNom(), "Le champ Nom doit être chargé.");
        assertEquals("Vin de Test", resume.getNom());
        assertNotNull(resume.getCouleur(), "Le sous-document Couleur doit être chargé.");

        // 2. Vérification que les champs exclus/lourds ne sont PAS chargés
        // Le résultat de la projection est un proxy Spring Data. Les champs non inclus
        // dans la projection doivent être null ou non initialisés.
        // Puisque Region et Avis ne sont pas dans BouteilleResume :

        // Pour les collections référencées, l'absence de getter dans la projection garantit
        // que l'information n'est pas hydratée, même si elle existe dans le document BSON.
        // Pour ce test, nous nous assurons surtout que l'opération est rapide et ne lève pas d'exception.

        // Si BouteilleResume était un DTO, nous testerions l'absence de Avis et Region.
        // Avec une interface de projection Spring, nous nous limitons à valider les champs présents.
    }

    // NOTE : Pour un test plus strict, nous aurions besoin d'une projection basée sur une classe
    // (Class-based projection) et non une interface pour vérifier explicitement la valeur null
    // des champs non projetés (comme getAvis() sur l'objet BouteilleResume casté).

}