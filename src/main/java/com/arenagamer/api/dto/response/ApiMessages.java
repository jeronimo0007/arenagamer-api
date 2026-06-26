package com.arenagamer.api.dto.response;

public final class ApiMessages {

    private ApiMessages() {}

    public static final String SUCCESS = "Operação realizada com sucesso";
    public static final String LIST_SUCCESS = "Listagem realizada com sucesso";
    public static final String FETCH_SUCCESS = "Consulta realizada com sucesso";
    public static final String CREATE_SUCCESS = "Registro criado com sucesso";
    public static final String UPDATE_SUCCESS = "Registro atualizado com sucesso";
    public static final String DELETE_SUCCESS = "Registro removido com sucesso";

    public static final String REGISTER_SUCCESS = "Usuário registrado com sucesso";
    public static final String LOGIN_SUCCESS = "Login realizado com sucesso";
    public static final String REFRESH_SUCCESS = "Token renovado com sucesso";
    public static final String LOGOUT_SUCCESS = "Logout realizado com sucesso";

    public static final String TOURNAMENT_CREATED = "Torneio criado com sucesso";
    public static final String TOURNAMENT_UPDATED = "Torneio atualizado com sucesso";
    public static final String TOURNAMENT_CANCELLED = "Torneio cancelado com sucesso";
    public static final String TOURNAMENT_JOINED = "Inscrição realizada com sucesso";
    public static final String TEAM_JOINED = "Time inscrito com sucesso";
    public static final String TOURNAMENT_WITHDRAWN = "Desinscrição realizada com sucesso";
    public static final String PARTICIPANT_REMOVED = "Participante removido com sucesso";
    public static final String BRACKET_GENERATED = "Chaves geradas com sucesso";
    public static final String MATCHES_SCHEDULED = "Partidas agendadas com sucesso";
    public static final String MATCH_RESCHEDULED = "Partida reagendada com sucesso";
    public static final String MATCH_RESULT_RECORDED = "Resultado registrado com sucesso";
    public static final String ROUND_ADVANCED = "Próxima fase gerada com sucesso";
    public static final String KNOCKOUT_GENERATED = "Mata-mata gerado com sucesso";
    public static final String TOURNAMENT_FINALIZED = "Torneio finalizado com sucesso";

    public static final String TEAM_CREATED = "Time criado com sucesso";
    public static final String TEAM_UPDATED = "Time atualizado com sucesso";
    public static final String TEAM_DELETED = "Time excluído com sucesso";
    public static final String TEAM_TRANSFERRED = "Time transferido com sucesso";
    public static final String TEAM_SETTINGS_UPDATED = "Configurações de times atualizadas";
    public static final String PROFILE_UPDATED = "Perfil atualizado com sucesso";
    public static final String MEMBER_ADDED = "Membro adicionado com sucesso";
    public static final String MEMBER_INVITED = "Convite enviado ao jogador";
    public static final String TEAM_JOIN_REQUEST_ACCEPTED = "Convite aceito — você entrou no time";
    public static final String ROSTER_VACANCY_FILLED = "Vaga na escalação preenchida";
    public static final String ROSTER_VACANCY_FORFEITED = "Vaga confirmada sem reposição — banimento aplicado";
    public static final String ROSTER_REALLOCATED = "Escalação atualizada com sucesso";
    public static final String MEMBER_REMOVED = "Membro removido com sucesso";
    public static final String CAPTAIN_SET = "Capitão definido com sucesso";
    public static final String AVAILABILITY_CHANGE_REQUESTED = "Solicitação de horários enviada ao dono do time";
    public static final String AVAILABILITY_CHANGE_APPROVED = "Horários do time atualizados";
    public static final String AVAILABILITY_CHANGE_REJECTED = "Solicitação de horários recusada";
    public static final String OWNERSHIP_TRANSFERRED = "Liderança transferida com sucesso";

    public static final String DEPOSIT_SUCCESS = "Depósito realizado com sucesso";
    public static final String WITHDRAW_SUCCESS = "Saque realizado com sucesso";
    public static final String CREDIT_INVOICE_CREATED = "Fatura de créditos gerada. O saldo será creditado após o pagamento.";
    public static final String ACCOUNT_DEACTIVATED = "Conta desativada com sucesso";

    public static final String PLAN_CREATED = "Plano criado com sucesso";
    public static final String PLAN_UPDATED = "Plano atualizado com sucesso";
    public static final String PLAN_DELETED = "Plano removido com sucesso";
    public static final String PRESET_CREATED = "Preset criado com sucesso";
    public static final String PRESET_UPDATED = "Preset atualizado com sucesso";
    public static final String SUBSCRIPTION_SUCCESS = "Plano contratado com sucesso";
    public static final String SUBSCRIPTION_PAID_WITH_CREDITS = "Plano contratado com sucesso usando créditos";
    public static final String SUBSCRIPTION_REMOVED = "Plano removido do cliente";
    public static final String SUBSCRIPTION_USAGE_RESET = "Uso mensal do plano resetado com sucesso";
    public static final String SUBSCRIPTION_CANCELLED = "Plano cancelado com sucesso";
    public static final String SUBSCRIPTION_CANCEL_SCHEDULED = "Cancelamento agendado para o fim do período atual";
    public static final String SUBSCRIPTION_DOWNGRADE_SCHEDULED = "Downgrade agendado para o fim do período atual";

    public static final String CREDIT_TIER_CREATED = "Tier de créditos criado com sucesso";
    public static final String CREDIT_TIER_UPDATED = "Tier de créditos atualizado com sucesso";
    public static final String CREDIT_TIER_DELETED = "Tier de créditos removido com sucesso";

    public static final String TOURNAMENT_PRICING_UPDATED = "Preços de torneio atualizados com sucesso";
}
