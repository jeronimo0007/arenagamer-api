ALTER TABLE tblclients
    ADD COLUMN nickname VARCHAR(50) NULL AFTER company,
    ADD COLUMN visibility ENUM('PUBLIC', 'PRIVATE', 'PROTECTED') NOT NULL DEFAULT 'PUBLIC' AFTER active;

CREATE INDEX idx_clients_visibility ON tblclients (visibility);

CREATE TABLE IF NOT EXISTS tblclient_ranks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_userid INT NOT NULL,
    preset_id BIGINT NOT NULL,
    rank_points INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_userid) REFERENCES tblclients(userid) ON DELETE CASCADE,
    FOREIGN KEY (preset_id) REFERENCES tblpresets(id),
    UNIQUE KEY uk_client_rank_preset (client_userid, preset_id),
    INDEX idx_client_ranks_client (client_userid),
    INDEX idx_client_ranks_preset (preset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
