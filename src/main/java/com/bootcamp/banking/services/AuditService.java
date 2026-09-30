package com.bootcamp.banking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bootcamp.banking.models.AuditRecord;
import com.bootcamp.banking.repos.AuditRepository;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public AuditRecord recordSuccess(String action, String customerId,
            String sourceAccountId, String destinationAccountId,
            BigDecimal amount) {
        return save(action, customerId, sourceAccountId,
                destinationAccountId, amount, "SUCCESS", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditRecord recordFailure(String action, String customerId,
            String sourceAccountId, String destinationAccountId,
            BigDecimal amount, String reason) {
        return save(action, customerId, sourceAccountId,
                destinationAccountId, amount, "REJECTED", reason);
    }

    private AuditRecord save(String action, String customerId,
            String sourceAccountId, String destinationAccountId,
            BigDecimal amount, String status, String reason) {
        return auditRepository.insert(new AuditRecord(null, action,
                customerId, sourceAccountId, destinationAccountId,
                amount, status, reason, LocalDateTime.now()));
    }

    public List<AuditRecord> getAllAudits() {
        return auditRepository.findAllByOrderByTimestampDesc();
    }

    public AuditRecord getAuditById(String auditId) {
        return auditRepository.findById(auditId).orElse(null);
    }

    public List<AuditRecord> getAuditsForAccount(String accountId) {
        return auditRepository
                .findBySourceAccountIdOrDestinationAccountIdOrderByTimestampDesc(
                        accountId, accountId);
    }
}
