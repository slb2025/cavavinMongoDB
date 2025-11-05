package org.example.cavavin.dal;

import org.example.cavavin.bo.Avis;
import org.example.cavavin.bo.Bouteille;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AvisRepository extends MongoRepository<Avis,String> {

    // Méthode de recherche pour trouver tous les avis d'une bouteille donnée.
    List<Avis> findByBouteille(Bouteille bouteille);

    // --- NOUVELLE MÉTHODE CRUCIALE POUR L'INTÉGRITÉ ---
    /**
     * Nécessaire pour implémenter la suppression en cascade au niveau de la couche Service.
     * Supprime tous les documents Avis référençant la Bouteille donnée.
     * @return le nombre d'avis supprimés (long) ou un simple void, selon le besoin.
     */
    void deleteByBouteille(Bouteille bouteille);
    // Alternativement, vous pouvez utiliser long deleteByBouteille(Bouteille bouteille); pour obtenir le compte.
}