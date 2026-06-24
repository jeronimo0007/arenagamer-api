CREATE TABLE IF NOT EXISTS tbltournament_entry_fees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    client_user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at DATETIME NULL,
    CONSTRAINT fk_tef_tournament FOREIGN KEY (tournament_id) REFERENCES tbltournaments (id),
    CONSTRAINT fk_tef_participant FOREIGN KEY (participant_id) REFERENCES tbltournament_participants (id),
    CONSTRAINT fk_tef_client FOREIGN KEY (client_user_id) REFERENCES tblclients (userid),
    CONSTRAINT uk_tef_participant UNIQUE (participant_id),
    INDEX idx_tef_tournament_status (tournament_id, status),
    INDEX idx_tef_client_tournament (client_user_id, tournament_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
