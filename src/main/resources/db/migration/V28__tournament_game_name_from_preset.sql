-- Nome do jogo nos torneios vem do preset

UPDATE tbltournaments t
INNER JOIN tblpresets p ON p.id = t.preset_id
SET t.game_name = p.game_name
WHERE t.preset_id IS NOT NULL;
