package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TournamentEntryFee;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.EntryFeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class TournamentEntryFeeItemResponse {

    private Long id;
    private Long participantId;

    @Schema(description = "Cliente (userid) que pagou a taxa de inscrição")
    private Integer payerClientUserId;

    private BigDecimal amount;
    private EntryFeeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime refundedAt;

    @Schema(description = "Preenchido quando a inscrição foi feita por equipe")
    private Long teamId;

    @Schema(description = "Nome do time inscrito")
    private String teamName;

    @Schema(description = "Tag do time inscrito")
    private String teamTag;

    @Schema(description = "Preenchido quando a inscrição foi solo")
    private Integer playerClientUserId;

    @Schema(description = "Apelido do jogador inscrito (solo)")
    private String playerNickname;

    public static TournamentEntryFeeItemResponse from(TournamentEntryFee fee) {
        TournamentParticipant participant = fee.getParticipant();
        TournamentEntryFeeItemResponseBuilder builder = TournamentEntryFeeItemResponse.builder()
                .id(fee.getId())
                .participantId(participant.getId())
                .payerClientUserId(fee.getClientUserId())
                .amount(fee.getAmount())
                .status(fee.getStatus())
                .createdAt(fee.getCreatedAt())
                .refundedAt(fee.getRefundedAt());

        if (participant.getTeam() != null) {
            builder.teamId(participant.getTeam().getId())
                    .teamName(participant.getTeam().getName())
                    .teamTag(participant.getTeam().getTag());
        } else if (participant.getContact() != null) {
            builder.playerClientUserId(participant.getContact().getUserid());
            if (participant.getContact().getClient() != null) {
                String nickname = participant.getContact().getClient().getNickname();
                if (nickname != null && !nickname.isBlank()) {
                    builder.playerNickname(nickname.trim());
                }
            }
        }

        return builder.build();
    }
}
