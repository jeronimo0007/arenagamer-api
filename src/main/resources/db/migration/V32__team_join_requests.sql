CREATE TABLE IF NOT EXISTS tblteam_join_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    client_user_id INT NOT NULL,
    invited_by_contact_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    CONSTRAINT fk_tjr_team FOREIGN KEY (team_id) REFERENCES tblteams (id),
    CONSTRAINT fk_tjr_client FOREIGN KEY (client_user_id) REFERENCES tblclients (userid),
    CONSTRAINT fk_tjr_contact FOREIGN KEY (invited_by_contact_id) REFERENCES tblcontacts (id),
    INDEX idx_tjr_team_status (team_id, status),
    INDEX idx_tjr_client_status (client_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
