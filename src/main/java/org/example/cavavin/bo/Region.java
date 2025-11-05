package org.example.cavavin.bo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Document("regions")
public class Region {
    @Id
    private String id;
    @Indexed(unique = true)
    @NonNull
    private String nomRegion;
}
