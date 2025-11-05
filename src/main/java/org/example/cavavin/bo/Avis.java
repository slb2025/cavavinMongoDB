package org.example.cavavin.bo;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Data
@RequiredArgsConstructor
@Document("avis")
public class Avis {
    @Id
    private String id;
    @NonNull
    private String commentaire;

    // NOUVEAU CHAMP : Référence inversée vers la bouteille parente
    // Cela permet de savoir à quelle bouteille appartient cet avis,
    // ce qui est crucial pour les requêtes.
    @NonNull
    @DocumentReference(lazy = true)
    private Bouteille bouteille;
}
