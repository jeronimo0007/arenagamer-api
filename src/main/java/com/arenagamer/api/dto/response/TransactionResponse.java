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
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Integer performedByContactId;
    private String performedByContactName;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        Contact performedBy = t.getPerformedBy();
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .status(t.getStatus())
                .description(t.getDescription())
                .performedByContactId(performedBy != null ? performedBy.getId() : null)
                .performedByContactName(performedBy != null
                        ? performedBy.getFirstname() + " " + performedBy.getLastname()
                        : null)
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
