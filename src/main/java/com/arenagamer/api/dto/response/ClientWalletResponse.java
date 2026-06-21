package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.service.WalletAccessService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWalletResponse {

    private Integer clientUserId;
    private Long walletId;
    private BigDecimal balance;
    private BigDecimal heldBalance;
    private BigDecimal availableBalance;
    private Boolean canView;
    private Boolean canUse;

    public static ClientWalletResponse from(Wallet wallet, Contact contact, WalletAccessService accessService) {
        return ClientWalletResponse.builder()
                .clientUserId(wallet.getClient().getUserId())
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .heldBalance(wallet.getHeldBalance())
                .availableBalance(wallet.getAvailableBalance())
                .canView(accessService.canViewWallet(contact))
                .canUse(accessService.canUseWallet(contact))
                .build();
    }
}
