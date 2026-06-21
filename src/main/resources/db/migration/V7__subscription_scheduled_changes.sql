ALTER TABLE tbluser_subscriptions
    ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pending_plan_id BIGINT NULL,
    ADD CONSTRAINT fk_subscriptions_pending_plan
        FOREIGN KEY (pending_plan_id) REFERENCES tblplans(id);
