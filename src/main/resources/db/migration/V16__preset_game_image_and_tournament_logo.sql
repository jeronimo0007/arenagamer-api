SET @preset_game_image_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tblpresets'
      AND COLUMN_NAME = 'game_image_url'
);

SET @preset_ddl := IF(
    @preset_game_image_exists = 0,
    'ALTER TABLE tblpresets ADD COLUMN game_image_url VARCHAR(500) NULL AFTER icon_url',
    'SELECT 1'
);

PREPARE preset_stmt FROM @preset_ddl;
EXECUTE preset_stmt;
DEALLOCATE PREPARE preset_stmt;

SET @tournament_logo_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbltournaments'
      AND COLUMN_NAME = 'logo_image_url'
);

SET @tournament_ddl := IF(
    @tournament_logo_exists = 0,
    'ALTER TABLE tbltournaments ADD COLUMN logo_image_url VARCHAR(500) NULL AFTER cover_image_url',
    'SELECT 1'
);

PREPARE tournament_stmt FROM @tournament_ddl;
EXECUTE tournament_stmt;
DEALLOCATE PREPARE tournament_stmt;

UPDATE tblpresets
SET game_image_url = icon_url
WHERE (game_image_url IS NULL OR game_image_url = '')
  AND icon_url IS NOT NULL
  AND icon_url <> '';
