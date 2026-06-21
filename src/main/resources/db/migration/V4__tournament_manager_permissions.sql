-- Permissões explícitas de gestão de torneio para contatos não-primários do cliente
CREATE TABLE IF NOT EXISTS tbltournament_managers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    contact_id INT NOT NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    UNIQUE KEY uk_tournament_manager (tournament_id, contact_id),
    INDEX idx_tournament_managers_contact (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tournaments_client ON tbltournaments(client_userid);
