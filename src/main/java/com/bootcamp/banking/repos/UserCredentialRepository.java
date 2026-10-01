package com.bootcamp.banking.repos;

import java.util.Optional;
import com.bootcamp.banking.models.UserCredential;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserCredentialRepository extends MongoRepository<UserCredential, String> {
    Optional<UserCredential> findByUsername(String username);
    boolean existsByUsername(String username);
}
