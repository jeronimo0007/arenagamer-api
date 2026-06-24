CREATE TABLE IF NOT EXISTS tbltournament_participant_players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    participant_id BIGINT NOT NULL,
    client_userid INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (participant_id) REFERENCES tbltournament_participants(id) ON DELETE CASCADE,
    FOREIGN KEY (client_userid) REFERENCES tblclients(userid),
    UNIQUE KEY uk_participant_player (participant_id, client_userid),
    INDEX idx_tpp_client (client_userid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tbltournament_participant_players (participant_id, client_userid)
SELECT tp.id, tm.client_userid
FROM tbltournament_participants tp
INNER JOIN tblteam_members tm ON tm.team_id = tp.team_id
WHERE tp.team_id IS NOT NULL
  AND tp.status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM tbltournament_participant_players tpp
      WHERE tpp.participant_id = tp.id
  );
