package com.arenagamer.api.entity.enums;

public enum EntryFeeStatus {
    /** Créditos retidos — reembolsável até a data limite. */
    HELD,
    /** Reembolsado ao cliente (desinscrição ou cancelamento). */
    REFUNDED,
    /** Capturado definitivamente (torneio iniciado). */
    CAPTURED
}
