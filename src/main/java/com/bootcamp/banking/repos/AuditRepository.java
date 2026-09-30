package com.bootcamp.banking.repos;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bootcamp.banking.models.AuditRecord;

public interface AuditRepository extends MongoRepository<AuditRecord, String> {

    List<AuditRecord> findAllByOrderByTimestampDesc();

    List<AuditRecord> findBySourceAccountIdOrDestinationAccountIdOrderByTimestampDesc(
            String sourceAccountId, String destinationAccountId);
}
