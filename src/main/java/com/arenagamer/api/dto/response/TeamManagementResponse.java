package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Painel de gestão do time")
public class TeamManagementResponse {

    private TeamResponse team;
    private java.util.List<TeamMemberClientResponse> members;
    @Schema(description = "Contato logado pode gerenciar (contato primário do cliente dono)")
    private Boolean canManage;
}
