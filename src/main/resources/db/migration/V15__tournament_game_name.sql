SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbltournaments'
      AND COLUMN_NAME = 'game_name'
);

SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE tbltournaments ADD COLUMN game_name VARCHAR(100) NULL AFTER name',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE tbltournaments t
    INNER JOIN tblpresets p ON p.id = t.preset_id
SET t.game_name = p.game_name
WHERE (t.game_name IS NULL OR t.game_name = '');
