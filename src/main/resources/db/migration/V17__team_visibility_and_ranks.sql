ALTER TABLE tblteams
    ADD COLUMN visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PUBLIC' AFTER active;

CREATE INDEX idx_teams_visibility ON tblteams (visibility);

CREATE TABLE IF NOT EXISTS tblteam_ranks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    preset_id BIGINT NOT NULL,
    rank_points INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES tblteams(id) ON DELETE CASCADE,
    FOREIGN KEY (preset_id) REFERENCES tblpresets(id),
    UNIQUE KEY uk_team_rank_preset (team_id, preset_id),
    INDEX idx_team_ranks_team (team_id),
    INDEX idx_team_ranks_preset (preset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
