package org.example.cavavin.dal;

import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.BouteilleResume;
import org.springframework.data.mongodb.repository.Aggregation; // <-- NOUVEAU
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BouteilleRepository extends MongoRepository<Bouteille, String> {

    /**
     * Optimise le chargement des bouteilles en effectuant un $lookup (join) sur la collection 'regions'.
     * Cela permet de pré-charger la région et d'éviter le problème N+1.
     */
    @Aggregation(pipeline = {
            // 1. Jointure : Effectue la jointure avec la collection 'regions'.
            // from: La collection à joindre ('regions', d'après le @Document de l'entité Region).
            // localField: Le champ de la collection Bouteille qui contient l'ID de la région (region_id).
            // foreignField: Le champ dans la collection regions qui correspond à l'ID (_id).
            // as: Le nom du champ qui contiendra le résultat de la jointure (doit correspondre au nom du champ 'region' dans Bouteille).
            "{ '$lookup' : { 'from' : 'regions', 'localField' : 'region_id', 'foreignField' : '_id', 'as' : 'region' } }",
            // 2. Unwind : Déconstruit le tableau 'region' résultant du lookup en objets simples.
            // Puisque region est 1-à-1, cela permet d'avoir un objet Region au lieu d'une liste d'un seul élément.
            "{ '$unwind' : '$region' }"
    })
    List<Bouteille> findAllWithRegionEagerly();

    /**
     * Optimisation: Récupère une liste des bouteilles en ne chargeant que les champs définis dans BouteilleResume.
     * C'est une requête plus rapide car moins de données sont transférées.
     */
    List<BouteilleResume> findAllBy();

    List<Bouteille> findByCouleur_Libelle(String libelle);

}