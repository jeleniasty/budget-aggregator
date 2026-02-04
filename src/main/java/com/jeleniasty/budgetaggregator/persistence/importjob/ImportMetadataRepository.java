package com.jeleniasty.budgetaggregator.persistence.importjob;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportMetadataRepository extends MongoRepository<ImportMetadata, String> {
}
