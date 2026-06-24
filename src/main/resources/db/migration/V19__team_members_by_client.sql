-- Membros do time passam a ser clientes (empresas), não contatos individuais.

ALTER TABLE tblteam_members
    ADD COLUMN client_userid INT NULL AFTER team_id;

UPDATE tblteam_members tm
INNER JOIN tblcontacts c ON c.id = tm.contact_id
SET tm.client_userid = c.userid;

ALTER TABLE tblteam_members
    MODIFY client_userid INT NOT NULL;

SET @member_contact_fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tblteam_members'
      AND COLUMN_NAME = 'contact_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_member_contact_fk := IF(
    @member_contact_fk IS NOT NULL,
    CONCAT('ALTER TABLE tblteam_members DROP FOREIGN KEY `', @member_contact_fk, '`'),
    'SELECT 1'
);
PREPARE stmt_member_fk FROM @drop_member_contact_fk;
EXECUTE stmt_member_fk;
DEALLOCATE PREPARE stmt_member_fk;

ALTER TABLE tblteam_members DROP INDEX uk_team_member;
ALTER TABLE tblteam_members DROP INDEX uk_team_member_pair;
ALTER TABLE tblteam_members DROP INDEX idx_team_members_contact;

ALTER TABLE tblteam_members
    DROP COLUMN contact_id,
    ADD UNIQUE KEY uk_team_member_client (team_id, client_userid),
    ADD INDEX idx_team_members_client (client_userid),
    ADD CONSTRAINT fk_team_members_client FOREIGN KEY (client_userid) REFERENCES tblclients(userid);
