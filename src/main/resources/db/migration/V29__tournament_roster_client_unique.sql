ALTER TABLE tbltournament_participant_players
    ADD COLUMN tournament_id BIGINT NULL AFTER participant_id;

UPDATE tbltournament_participant_players tpp
INNER JOIN tbltournament_participants tp ON tp.id = tpp.participant_id
SET tpp.tournament_id = tp.tournament_id;

ALTER TABLE tbltournament_participant_players
    MODIFY tournament_id BIGINT NOT NULL,
    ADD INDEX idx_tpp_tournament (tournament_id),
    ADD CONSTRAINT fk_tpp_tournament FOREIGN KEY (tournament_id) REFERENCES tbltournaments(id),
    ADD UNIQUE KEY uk_tournament_client_roster (tournament_id, client_userid);
