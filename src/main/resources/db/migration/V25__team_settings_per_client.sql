-- Limites por cliente (empresa) e torneios simultâneos por cliente.
ALTER TABLE tblteam_settings
    ADD COLUMN max_tournaments_per_client INT NULL AFTER max_tournaments_per_team;

ALTER TABLE tblteam_settings
    CHANGE COLUMN max_owned_teams_per_contact max_owned_teams_per_client INT NOT NULL DEFAULT 1,
    CHANGE COLUMN max_participated_teams_per_contact max_participated_teams_per_client INT NOT NULL DEFAULT 3;
