ALTER TABLE tbluser_subscriptions
    ADD COLUMN tournaments_usage_month VARCHAR(7) NULL AFTER tournaments_used_this_month;
