package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TournamentManagerPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentManagerResponse {

    private Integer contactId;
    private String contactName;
    private String contactEmail;
    private LocalDateTime grantedAt;

    public static TournamentManagerResponse from(TournamentManagerPermission permission) {
        return TournamentManagerResponse.builder()
                .contactId(permission.getContact().getId())
                .contactName(permission.getContact().getFirstname() + " " + permission.getContact().getLastname())
                .contactEmail(permission.getContact().getEmail())
                .grantedAt(permission.getGrantedAt())
                .build();
    }
}
