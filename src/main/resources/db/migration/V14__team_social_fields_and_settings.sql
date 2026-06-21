-- Campos sociais e regras do time
ALTER TABLE tblteams
    ADD COLUMN youtube_url VARCHAR(500) NULL AFTER logo_url,
    ADD COLUMN instagram_url VARCHAR(500) NULL AFTER youtube_url,
    ADD COLUMN twitch_url VARCHAR(500) NULL AFTER instagram_url,
    ADD COLUMN other_social_url VARCHAR(500) NULL AFTER twitch_url,
    ADD COLUMN rules_change TEXT NULL AFTER other_social_url;

-- Redes sociais no perfil do contato
ALTER TABLE tblcontacts
    ADD COLUMN instagram_url VARCHAR(500) NULL AFTER profile_image,
    ADD COLUMN youtube_url VARCHAR(500) NULL AFTER instagram_url,
    ADD COLUMN twitch_url VARCHAR(500) NULL AFTER youtube_url;

-- Configurações globais de times (singleton)
CREATE TABLE tblteam_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    max_owned_teams_per_contact INT NOT NULL DEFAULT 1,
    max_participated_teams_per_contact INT NOT NULL DEFAULT 3,
    max_tournaments_per_team INT NULL,
    CONSTRAINT chk_team_settings_singleton CHECK (id = 1)
);

INSERT INTO tblteam_settings (id, max_owned_teams_per_contact, max_participated_teams_per_contact, max_tournaments_per_team)
VALUES (1, 1, 3, NULL);

-- Permite N participações em times (remove limite global de 1 membership)
ALTER TABLE tblteam_members DROP INDEX uk_team_member_contact;

-- Um contato só pode ser dono de um time
ALTER TABLE tblteams ADD UNIQUE KEY uk_team_owner_contact (owner_contact_id);

-- Evita duplicar membro no mesmo time
ALTER TABLE tblteam_members ADD UNIQUE KEY uk_team_member_pair (team_id, contact_id);
