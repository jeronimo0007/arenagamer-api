-- Tamanho máximo da equipe por torneio (somente format = TEAM).
ALTER TABLE tbltournaments
    ADD COLUMN max_players_per_team INT NULL AFTER min_participants;

UPDATE tbltournaments t
INNER JOIN tblpresets p ON p.id = t.preset_id
SET t.max_players_per_team = p.max_players_per_team
WHERE t.format = 'TEAM'
  AND t.max_players_per_team IS NULL
  AND p.max_players_per_team IS NOT NULL;
