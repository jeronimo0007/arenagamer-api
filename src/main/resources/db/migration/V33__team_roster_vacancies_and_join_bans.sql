ALTER TABLE tblteam_settings
    ADD COLUMN team_join_ban_days_after_unreplaced_exit INT NOT NULL DEFAULT 7
        AFTER max_tournaments_per_client;

CREATE TABLE IF NOT EXISTS tblteam_join_bans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_user_id INT NOT NULL,
    reason VARCHAR(500) NULL,
    banned_until DATETIME NOT NULL,
    roster_vacancy_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tjb_client FOREIGN KEY (client_user_id) REFERENCES tblclients (userid),
    INDEX idx_tjb_client_until (client_user_id, banned_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblteam_roster_vacancies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    exited_client_user_id INT NOT NULL,
    replacement_client_user_id INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    CONSTRAINT fk_trv_team FOREIGN KEY (team_id) REFERENCES tblteams (id),
    CONSTRAINT fk_trv_tournament FOREIGN KEY (tournament_id) REFERENCES tbltournaments (id),
    CONSTRAINT fk_trv_participant FOREIGN KEY (participant_id) REFERENCES tbltournament_participants (id),
    CONSTRAINT fk_trv_exited_client FOREIGN KEY (exited_client_user_id) REFERENCES tblclients (userid),
    CONSTRAINT fk_trv_replacement_client FOREIGN KEY (replacement_client_user_id) REFERENCES tblclients (userid),
    INDEX idx_trv_team_status (team_id, status),
    INDEX idx_trv_participant_status (participant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE tblteam_join_bans
    ADD CONSTRAINT fk_tjb_vacancy FOREIGN KEY (roster_vacancy_id) REFERENCES tblteam_roster_vacancies (id);
