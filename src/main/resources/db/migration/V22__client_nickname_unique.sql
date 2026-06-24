UPDATE tblclients
SET nickname = TRIM(company)
WHERE nickname IS NULL OR TRIM(nickname) = '';

UPDATE tblclients c
INNER JOIN (
    SELECT LOWER(TRIM(nickname)) AS nick_key, MIN(userid) AS keep_userid
    FROM tblclients
    WHERE nickname IS NOT NULL AND TRIM(nickname) <> ''
    GROUP BY LOWER(TRIM(nickname))
    HAVING COUNT(*) > 1
) d ON LOWER(TRIM(c.nickname)) = d.nick_key AND c.userid <> d.keep_userid
SET c.nickname = CONCAT(LEFT(TRIM(c.nickname), 40), '_', c.userid);

ALTER TABLE tblclients
    ADD UNIQUE KEY uk_clients_nickname (nickname);
