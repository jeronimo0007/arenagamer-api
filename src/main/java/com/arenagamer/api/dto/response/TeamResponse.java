package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.Contact;
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
    private String youtubeUrl;
    private String instagramUrl;
    private String twitchUrl;
    private String otherSocialUrl;
    private String rulesChange;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static TeamResponse from(Team t) {
        Contact owner = t.getOwner();
        String ownerName = null;
        Long ownerId = null;
        if (owner != null) {
            ownerId = Long.valueOf(owner.getId());
            String first = owner.getFirstname() != null ? owner.getFirstname().trim() : "";
            String last = owner.getLastname() != null ? owner.getLastname().trim() : "";
            ownerName = (first + " " + last).trim();
            if (ownerName.isEmpty()) {
                ownerName = null;
            }
        }

        return TeamResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .tag(t.getTag())
                .logoUrl(t.getLogoUrl())
                .youtubeUrl(t.getYoutubeUrl())
                .instagramUrl(t.getInstagramUrl())
                .twitchUrl(t.getTwitchUrl())
                .otherSocialUrl(t.getOtherSocialUrl())
                .rulesChange(t.getRulesChange())
                .ownerId(ownerId)
                .ownerName(ownerName)
                .memberCount(t.getMembers() != null ? t.getMembers().size() : 0)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
