package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {

    private Long id;
    private String name;
    private String tag;
    private String logoUrl;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static TeamResponse from(Team t) {
        return TeamResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .tag(t.getTag())
                .logoUrl(t.getLogoUrl())
                .ownerId(t.getOwner().getId())
                .ownerName(t.getOwner().getFirstName() + " " + t.getOwner().getLastName())
                .memberCount(t.getMembers() != null ? t.getMembers().size() : 0)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
