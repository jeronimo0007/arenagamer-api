ALTER TABLE tbluser_subscriptions
    ADD COLUMN billing_period_months INT NOT NULL DEFAULT 1;

UPDATE tbluser_subscriptions
SET billing_period_months = 12
WHERE TIMESTAMPDIFF(MONTH, starts_at, expires_at) >= 11;

UPDATE tbluser_subscriptions
SET billing_period_months = 6
WHERE billing_period_months = 1
  AND TIMESTAMPDIFF(MONTH, starts_at, expires_at) BETWEEN 5 AND 10;
