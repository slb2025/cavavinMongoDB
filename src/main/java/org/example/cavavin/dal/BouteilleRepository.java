package org.example.cavavin.dal;

import org.example.cavavin.bo.Bouteille;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BouteilleRepository extends MongoRepository<Bouteille,String> {

}
