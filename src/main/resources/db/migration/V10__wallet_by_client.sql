ALTER TABLE tblcontacts
    ADD COLUMN wallet_view_allowed TINYINT(1) NOT NULL DEFAULT 0 AFTER is_primary,
    ADD COLUMN wallet_use_allowed TINYINT(1) NOT NULL DEFAULT 0 AFTER wallet_view_allowed;

ALTER TABLE tbltransactions
    ADD COLUMN performed_by_contact_id INT NULL AFTER wallet_id,
    ADD INDEX idx_transactions_performed_by (performed_by_contact_id);

ALTER TABLE tblwallets
    ADD COLUMN client_userid INT NULL AFTER id;

UPDATE tblwallets w
INNER JOIN tblcontacts c ON c.id = w.contact_id
SET w.client_userid = c.userid;

UPDATE tbltransactions t
INNER JOIN tblwallets w ON w.id = t.wallet_id
SET t.performed_by_contact_id = w.contact_id
WHERE t.performed_by_contact_id IS NULL;

CREATE TEMPORARY TABLE tmp_wallet_merge AS
SELECT
    client_userid,
    MIN(id) AS keep_id,
    SUM(balance) AS total_balance,
    SUM(held_balance) AS total_held
FROM tblwallets
WHERE client_userid IS NOT NULL
GROUP BY client_userid;

UPDATE tblwallets w
INNER JOIN tmp_wallet_merge m ON w.id = m.keep_id
SET w.balance = m.total_balance,
    w.held_balance = m.total_held;

UPDATE tbltransactions t
INNER JOIN tblwallets w ON t.wallet_id = w.id
INNER JOIN tmp_wallet_merge m ON w.client_userid = m.client_userid
SET t.wallet_id = m.keep_id
WHERE w.id <> m.keep_id;

DELETE w FROM tblwallets w
INNER JOIN tmp_wallet_merge m ON w.client_userid = m.client_userid
WHERE w.id <> m.keep_id;

DROP TEMPORARY TABLE tmp_wallet_merge;

SET @wallet_contact_fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tblwallets'
      AND COLUMN_NAME = 'contact_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_wallet_contact_fk := IF(
    @wallet_contact_fk IS NOT NULL,
    CONCAT('ALTER TABLE tblwallets DROP FOREIGN KEY `', @wallet_contact_fk, '`'),
    'SELECT 1'
);
PREPARE stmt_wallet_fk FROM @drop_wallet_contact_fk;
EXECUTE stmt_wallet_fk;
DEALLOCATE PREPARE stmt_wallet_fk;

ALTER TABLE tblwallets
    DROP INDEX contact_id,
    DROP COLUMN contact_id;

ALTER TABLE tblwallets
    MODIFY client_userid INT NOT NULL,
    ADD UNIQUE INDEX idx_wallets_client_userid (client_userid),
    ADD CONSTRAINT fk_wallets_client FOREIGN KEY (client_userid) REFERENCES tblclients(userid);

ALTER TABLE tbltransactions
    ADD CONSTRAINT fk_transactions_performed_by FOREIGN KEY (performed_by_contact_id) REFERENCES tblcontacts(id);
