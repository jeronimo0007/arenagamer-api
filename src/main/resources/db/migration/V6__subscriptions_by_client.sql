-- Assinaturas vinculadas ao cliente (userid), não ao contato individual

ALTER TABLE tbluser_subscriptions
    ADD COLUMN client_user_id INT NULL AFTER id;

UPDATE tbluser_subscriptions s
INNER JOIN tblcontacts c ON c.id = s.contact_id
SET s.client_user_id = c.userid;

DELETE FROM tbluser_subscriptions WHERE client_user_id IS NULL;

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbluser_subscriptions'
      AND COLUMN_NAME = 'contact_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_fk = IF(
    @fk_name IS NOT NULL,
    CONCAT('ALTER TABLE tbluser_subscriptions DROP FOREIGN KEY `', @fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE tbluser_subscriptions
    DROP INDEX idx_subscriptions_contact,
    DROP COLUMN contact_id;

ALTER TABLE tbluser_subscriptions
    MODIFY client_user_id INT NOT NULL,
    ADD INDEX idx_subscriptions_client (client_user_id),
    ADD CONSTRAINT fk_subscriptions_client
        FOREIGN KEY (client_user_id) REFERENCES tblclients(userid);
