package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminClientWalletResponse {

    private Integer clientUserId;
    private String companyName;
    private WalletResponse wallet;

    public static AdminClientWalletResponse from(Client client, Wallet wallet) {
        return AdminClientWalletResponse.builder()
                .clientUserId(client.getUserId())
                .companyName(client.getCompany())
                .wallet(wallet != null ? WalletResponse.from(wallet) : null)
                .build();
    }
}
