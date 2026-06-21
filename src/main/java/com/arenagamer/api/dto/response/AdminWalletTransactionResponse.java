package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.enums.TransactionStatus;
import com.arenagamer.api.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminWalletTransactionResponse {

    private Long id;
    private Integer performedByContactId;
    private String performedByContactName;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String referenceType;
    private Long referenceId;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public static AdminWalletTransactionResponse from(Transaction transaction) {
        Contact performedBy = transaction.getPerformedBy();
        return AdminWalletTransactionResponse.builder()
                .id(transaction.getId())
                .performedByContactId(performedBy != null ? performedBy.getId() : null)
                .performedByContactName(performedBy != null
                        ? performedBy.getFirstname() + " " + performedBy.getLastname()
                        : null)
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
