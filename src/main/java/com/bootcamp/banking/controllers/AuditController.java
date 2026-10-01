package com.bootcamp.banking.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bootcamp.banking.models.AuditRecord;
import com.bootcamp.banking.services.AuditService;

@RestController
@RequestMapping("/api/audits")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditRecord> getAllAudits() {
        return auditService.getAllAudits();
    }

    @GetMapping("/account/{accountId}")
    public List<AuditRecord> getAuditsForAccount(
            @PathVariable String accountId) {
        return auditService.getAuditsForAccount(accountId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditRecord> getAuditById(@PathVariable String id) {
        AuditRecord audit = auditService.getAuditById(id);
        return audit == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(audit);
    }
}
