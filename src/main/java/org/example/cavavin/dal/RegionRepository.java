package org.example.cavavin.dal;

import org.example.cavavin.bo.Region;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegionRepository extends MongoRepository<Region, String> {
}
