-- Dono do time passa a ser o cliente (userid), não o contato individual.

ALTER TABLE tblteams
    ADD COLUMN client_userid INT NULL AFTER owner_contact_id;

UPDATE tblteams t
INNER JOIN tblcontacts c ON c.id = t.owner_contact_id
SET t.client_userid = c.userid;

ALTER TABLE tblteams
    MODIFY client_userid INT NOT NULL;

SET @owner_fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tblteams'
      AND COLUMN_NAME = 'owner_contact_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_owner_fk := IF(
    @owner_fk IS NOT NULL,
    CONCAT('ALTER TABLE tblteams DROP FOREIGN KEY `', @owner_fk, '`'),
    'SELECT 1'
);
PREPARE stmt_owner_fk FROM @drop_owner_fk;
EXECUTE stmt_owner_fk;
DEALLOCATE PREPARE stmt_owner_fk;

ALTER TABLE tblteams DROP INDEX uk_team_owner_contact;

ALTER TABLE tblteams
    DROP COLUMN owner_contact_id,
    ADD UNIQUE KEY uk_team_owner_client (client_userid),
    ADD INDEX idx_teams_client (client_userid),
    ADD CONSTRAINT fk_teams_client FOREIGN KEY (client_userid) REFERENCES tblclients(userid);
