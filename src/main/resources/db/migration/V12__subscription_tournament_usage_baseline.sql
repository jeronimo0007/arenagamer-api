ALTER TABLE tbluser_subscriptions
    ADD COLUMN tournaments_usage_baseline INT NOT NULL DEFAULT 0 AFTER tournaments_usage_month;
