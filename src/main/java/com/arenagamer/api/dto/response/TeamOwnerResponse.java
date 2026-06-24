package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cliente dono do time")
public class TeamOwnerResponse {

    private Integer clientUserId;
    private String clientName;
    private String state;

    public static TeamOwnerResponse from(Client client) {
        if (client == null) {
            return null;
        }
        return TeamOwnerResponse.builder()
                .clientUserId(client.getUserId())
                .clientName(client.getCompany())
                .state(client.getState())
                .build();
    }
}
