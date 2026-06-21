CREATE TABLE IF NOT EXISTS tbltournament_pricing_settings (
    id BIGINT PRIMARY KEY,
    base_tournament_price DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    extra_participant_price DECIMAL(10,2) NOT NULL DEFAULT 1.00,
    included_participants INT NOT NULL DEFAULT 8
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tbltournament_pricing_settings (id, base_tournament_price, extra_participant_price, included_participants)
SELECT 1, 5.00, 1.00, 8
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tbltournament_pricing_settings WHERE id = 1);
