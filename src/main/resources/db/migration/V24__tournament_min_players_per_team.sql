-- Tamanho mínimo da equipe por torneio (somente format = TEAM).
ALTER TABLE tbltournaments
    ADD COLUMN min_players_per_team INT NULL AFTER min_participants;

UPDATE tbltournaments t
INNER JOIN tblpresets p ON p.id = t.preset_id
SET t.min_players_per_team = p.min_players_per_team
WHERE t.format = 'TEAM'
  AND t.min_players_per_team IS NULL
  AND p.min_players_per_team IS NOT NULL;
