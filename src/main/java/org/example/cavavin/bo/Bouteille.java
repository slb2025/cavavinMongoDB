package org.example.cavavin.bo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

@RequiredArgsConstructor
@NoArgsConstructor
@Data
@Document("bouteilles")

public class Bouteille {
    @Id
    private String id;

    @Field("nom")
    @Indexed(unique = true)
    @NonNull
    private String nom;
    @NonNull
    private Integer millesime;

    //Stratégie2
    @DocumentReference
    @Field("region_id")
    private Region region;

    private Couleur couleur;

}
