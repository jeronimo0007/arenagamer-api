-- ArenaGamer API - Tabelas da Arena (v2)
-- Executada após baseline do banco Perfex compartilhado.
-- Usa tabelas existentes: tblstaff, tblclients, tblcontacts

-- =====================
-- AUTH SESSIONS
-- =====================
CREATE TABLE IF NOT EXISTS tblarena_refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_type ENUM('STAFF','CONTACT') NOT NULL,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_arena_refresh_token (refresh_token),
    INDEX idx_arena_refresh_user (user_type, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- PLANS & CREDITS
-- =====================
CREATE TABLE IF NOT EXISTS tblplans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    free_tournaments_per_month INT NOT NULL DEFAULT 0,
    free_max_participants INT NOT NULL DEFAULT 0,
    allows_entry_fee BOOLEAN DEFAULT FALSE,
    max_tournaments_per_month INT,
    monthly_price DECIMAL(10,2) DEFAULT 0.00,
    hidden BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblcredit_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    min_participants INT NOT NULL,
    max_participants INT NOT NULL,
    credit_cost DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbluser_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_id INT NOT NULL,
    plan_id BIGINT NOT NULL,
    starts_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    tournaments_used_this_month INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    FOREIGN KEY (plan_id) REFERENCES tblplans(id),
    INDEX idx_subscriptions_contact (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- WALLET & TRANSACTIONS
-- =====================
CREATE TABLE IF NOT EXISTS tblwallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_id INT NOT NULL UNIQUE,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    held_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    INDEX idx_wallets_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbltransactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    type ENUM('DEPOSIT','WITHDRAWAL','TOURNAMENT_FEE','TOURNAMENT_PRIZE','ENTRY_FEE','REFUND','HOLD','HOLD_RELEASE','HOLD_CAPTURE') NOT NULL,
    status ENUM('PENDING','COMPLETED','FAILED','CANCELLED','HELD','RELEASED') NOT NULL DEFAULT 'PENDING',
    reference_type VARCHAR(50),
    reference_id BIGINT,
    description TEXT,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES tblwallets(id),
    INDEX idx_transactions_wallet_id (wallet_id),
    INDEX idx_transactions_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- PRESETS & POSITIONS
-- =====================
CREATE TABLE IF NOT EXISTS tblpresets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_name VARCHAR(100) NOT NULL,
    platform VARCHAR(100),
    team_size INT NOT NULL DEFAULT 1,
    min_players_per_team INT NOT NULL DEFAULT 1,
    max_players_per_team INT NOT NULL DEFAULT 1,
    icon_url VARCHAR(500),
    rules_template TEXT,
    scoring_script TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblpositions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    preset_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    abbreviation VARCHAR(10),
    sort_order INT DEFAULT 0,
    FOREIGN KEY (preset_id) REFERENCES tblpresets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- TEAMS
-- =====================
CREATE TABLE IF NOT EXISTS tblteams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tag VARCHAR(20),
    logo_url VARCHAR(500),
    owner_contact_id INT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_contact_id) REFERENCES tblcontacts(id),
    INDEX idx_teams_owner (owner_contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblteam_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    contact_id INT NOT NULL,
    position VARCHAR(50),
    is_captain BOOLEAN DEFAULT FALSE,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES tblteams(id),
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    UNIQUE KEY uk_team_member (team_id, contact_id),
    INDEX idx_team_members_team (team_id),
    INDEX idx_team_members_contact (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- AVAILABILITY
-- =====================
CREATE TABLE IF NOT EXISTS tblavailability_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_id INT,
    team_id BIGINT,
    prefer_weekends BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    FOREIGN KEY (team_id) REFERENCES tblteams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblavailability_time_windows (
    profile_id BIGINT NOT NULL,
    time_window ENUM('MORNING','AFTERNOON','EVENING','NIGHT') NOT NULL,
    PRIMARY KEY (profile_id, time_window),
    FOREIGN KEY (profile_id) REFERENCES tblavailability_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblprecise_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    availability_profile_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    FOREIGN KEY (availability_profile_id) REFERENCES tblavailability_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- TOURNAMENTS
-- =====================
CREATE TABLE IF NOT EXISTS tbltournaments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_type ENUM('STAFF','CONTACT') NOT NULL,
    owner_id BIGINT NOT NULL,
    client_userid INT,
    preset_id BIGINT,
    type ENUM('SINGLE_ELIMINATION','DOUBLE_ELIMINATION','ROUND_ROBIN','GROUP_STAGE','SWISS') NOT NULL,
    format ENUM('SOLO','TEAM') NOT NULL,
    visibility ENUM('PUBLIC','PRIVATE') NOT NULL DEFAULT 'PUBLIC',
    status ENUM('DRAFT','REGISTRATION_OPEN','REGISTRATION_CLOSED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    participants_limit INT NOT NULL,
    min_participants INT,
    entry_fee_credits DECIMAL(10,2) DEFAULT 0.00,
    fee_percentage DECIMAL(5,2) DEFAULT 0.00,
    prize_pool DECIMAL(15,2) DEFAULT 0.00,
    prize_type ENUM('AUTOMATIC','MANUAL') DEFAULT 'MANUAL',
    groups_count INT,
    teams_per_group INT,
    advance_per_group INT,
    best_of INT DEFAULT 1,
    rules TEXT,
    tiebreaker_rules TEXT,
    start_date DATETIME,
    registration_deadline DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_userid) REFERENCES tblclients(userid),
    FOREIGN KEY (preset_id) REFERENCES tblpresets(id),
    INDEX idx_tournaments_slug (slug),
    INDEX idx_tournaments_owner (owner_type, owner_id),
    INDEX idx_tournaments_status (status),
    INDEX idx_tournaments_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbltournament_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    contact_id INT,
    team_id BIGINT,
    status ENUM('PENDING','APPROVED','REJECTED','WITHDRAWN','KICKED') NOT NULL DEFAULT 'PENDING',
    seed_number INT,
    group_number INT,
    availability_profile_id BIGINT,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id),
    FOREIGN KEY (contact_id) REFERENCES tblcontacts(id),
    FOREIGN KEY (team_id) REFERENCES tblteams(id),
    FOREIGN KEY (availability_profile_id) REFERENCES tblavailability_profiles(id),
    INDEX idx_tp_tournament (tournament_id),
    INDEX idx_tp_contact (contact_id),
    INDEX idx_tp_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- ROUNDS & MATCHES
-- =====================
CREATE TABLE IF NOT EXISTS tblrounds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    round_number INT NOT NULL,
    type ENUM('GROUP_STAGE','KNOCKOUT','FINAL','THIRD_PLACE','SEMIFINAL','QUARTERFINAL') NOT NULL,
    status ENUM('PENDING','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'PENDING',
    group_number INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id),
    INDEX idx_rounds_tournament (tournament_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblmatches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id BIGINT NOT NULL,
    home_participant_id BIGINT,
    away_participant_id BIGINT,
    match_number INT,
    scheduled_at DATETIME,
    time_window ENUM('MORNING','AFTERNOON','EVENING','NIGHT'),
    home_score INT,
    away_score INT,
    winner_participant_id BIGINT,
    status ENUM('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','WALKOVER','RESCHEDULED') NOT NULL DEFAULT 'SCHEDULED',
    next_match_id BIGINT,
    bracket_position INT,
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (round_id) REFERENCES tblrounds(id),
    FOREIGN KEY (home_participant_id) REFERENCES tbltournament_participants(id),
    FOREIGN KEY (away_participant_id) REFERENCES tbltournament_participants(id),
    FOREIGN KEY (winner_participant_id) REFERENCES tbltournament_participants(id),
    INDEX idx_matches_round (round_id),
    INDEX idx_matches_scheduled (scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblbracket_seeds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    seed_number INT NOT NULL,
    FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id),
    FOREIGN KEY (participant_id) REFERENCES tbltournament_participants(id),
    UNIQUE KEY uk_bracket_seed (tournament_id, seed_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblgroup_standings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    group_number INT NOT NULL,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    points INT DEFAULT 0,
    games_played INT DEFAULT 0,
    score_for INT DEFAULT 0,
    score_against INT DEFAULT 0,
    goal_difference INT DEFAULT 0,
    rank_position INT,
    FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id),
    FOREIGN KEY (participant_id) REFERENCES tbltournament_participants(id),
    INDEX idx_gs_tournament_group (tournament_id, group_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- AUDIT, WEBHOOKS, OAUTH, API KEYS
-- =====================
CREATE TABLE IF NOT EXISTS tblaudit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_type ENUM('STAFF','CONTACT') NULL,
    actor_id BIGINT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    old_value JSON,
    new_value JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_actor (actor_type, actor_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblwebhook_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_type ENUM('STAFF','CONTACT') NOT NULL,
    owner_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    event_types JSON NOT NULL,
    secret VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webhook_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblarena_oauth_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    redirect_uri VARCHAR(500),
    scopes VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblapi_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_type ENUM('STAFF','CONTACT') NOT NULL,
    owner_id BIGINT NOT NULL,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    scopes VARCHAR(500),
    last_used_at DATETIME,
    expires_at DATETIME,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_keys_key (api_key),
    INDEX idx_api_keys_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- SEED DATA (apenas se tabelas estiverem vazias)
-- =====================
INSERT INTO tblplans (name, description, free_tournaments_per_month, free_max_participants, allows_entry_fee, monthly_price, sort_order)
SELECT * FROM (
    SELECT 'Free' AS name, 'Plano gratuito com limites básicos' AS description, 2 AS free_tournaments_per_month, 16 AS free_max_participants, FALSE AS allows_entry_fee, 0.00 AS monthly_price, 1 AS sort_order
    UNION ALL SELECT 'Pro', 'Plano profissional para organizadores', 10, 64, TRUE, 29.90, 2
    UNION ALL SELECT 'Enterprise', 'Plano empresarial sem limites', 999, 999, TRUE, 99.90, 3
) AS seed
WHERE (SELECT COUNT(*) FROM tblplans) = 0;

INSERT INTO tblcredit_tiers (min_participants, max_participants, credit_cost)
SELECT * FROM (
    SELECT 2 AS min_participants, 8 AS max_participants, 5.00 AS credit_cost
    UNION ALL SELECT 9, 16, 10.00
    UNION ALL SELECT 17, 32, 18.00
    UNION ALL SELECT 33, 64, 30.00
    UNION ALL SELECT 65, 128, 50.00
    UNION ALL SELECT 129, 256, 80.00
) AS seed
WHERE (SELECT COUNT(*) FROM tblcredit_tiers) = 0;

INSERT INTO tblpresets (game_name, platform, team_size, min_players_per_team, max_players_per_team)
SELECT * FROM (
    SELECT 'Counter-Strike 2' AS game_name, 'PC' AS platform, 5 AS team_size, 5 AS min_players_per_team, 7 AS max_players_per_team
    UNION ALL SELECT 'League of Legends', 'PC', 5, 5, 7
    UNION ALL SELECT 'Valorant', 'PC', 5, 5, 6
    UNION ALL SELECT 'FIFA', 'Multi', 1, 1, 1
    UNION ALL SELECT 'Fortnite', 'Multi', 1, 1, 4
    UNION ALL SELECT 'Free Fire', 'Mobile', 4, 4, 5
    UNION ALL SELECT 'PUBG Mobile', 'Mobile', 4, 4, 5
) AS seed
WHERE (SELECT COUNT(*) FROM tblpresets) = 0;
