ALTER TABLE tblteams
    ADD COLUMN banner_url VARCHAR(500) NULL AFTER logo_url,
    ADD COLUMN description TEXT NULL AFTER other_social_url;

UPDATE tblteams
SET description = rules_change
WHERE rules_change IS NOT NULL
  AND description IS NULL;

ALTER TABLE tblteams
    MODIFY COLUMN visibility ENUM('PUBLIC', 'PRIVATE', 'PROTECTED') NOT NULL DEFAULT 'PUBLIC';
