package com.bootcamp.banking.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "audits")
public class AuditRecord {

    @Id
    private String auditId;
    private String action;
    private String customerId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private LocalDateTime timestamp;

    public AuditRecord() {
    }

    public AuditRecord(String auditId, String action, String customerId,
            String sourceAccountId, String destinationAccountId,
            BigDecimal amount, String status, String reason,
            LocalDateTime timestamp) {
        this.auditId = auditId;
        this.action = action;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
