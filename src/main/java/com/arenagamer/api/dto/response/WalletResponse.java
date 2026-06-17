package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {

    private Long id;
    private BigDecimal balance;
    private BigDecimal heldBalance;
    private BigDecimal availableBalance;

    public static WalletResponse from(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .balance(w.getBalance())
                .heldBalance(w.getHeldBalance())
                .availableBalance(w.getAvailableBalance())
                .build();
    }
}
