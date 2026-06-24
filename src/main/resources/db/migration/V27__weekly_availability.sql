-- Disponibilidade recorrente por dia da semana (jogador e time)

ALTER TABLE tblavailability_profiles
    ADD COLUMN client_userid INT NULL AFTER contact_id,
    ADD UNIQUE KEY uk_availability_client (client_userid),
    ADD UNIQUE KEY uk_availability_team (team_id);

CREATE TABLE IF NOT EXISTS tblweekly_availability_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    availability_profile_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (availability_profile_id) REFERENCES tblavailability_profiles(id) ON DELETE CASCADE,
    INDEX idx_weekly_slots_profile (availability_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblteam_availability_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    requested_by_contact_id INT NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    FOREIGN KEY (team_id) REFERENCES tblteams(id),
    FOREIGN KEY (requested_by_contact_id) REFERENCES tblcontacts(id),
    INDEX idx_team_avail_req_team_status (team_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblteam_availability_request_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (request_id) REFERENCES tblteam_availability_change_requests(id) ON DELETE CASCADE,
    INDEX idx_team_avail_req_slots_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
