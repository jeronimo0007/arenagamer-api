package com.arenagamer.api.entity.enums;

/**
 * Filtros da listagem pública de torneios.
 * UPCOMING: torneios públicos com data prevista de abertura das inscrições preenchida.
 */
public enum PublicTournamentFilter {
    REGISTRATION_OPEN,
    UPCOMING,
    IN_PROGRESS,
    FINISHED,
    CANCELLED
}
