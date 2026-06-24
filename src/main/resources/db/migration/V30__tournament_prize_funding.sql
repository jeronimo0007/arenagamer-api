ALTER TABLE tbltournaments
    ADD COLUMN prize_funding VARCHAR(20) NOT NULL DEFAULT 'FIXED' AFTER prize_type;

UPDATE tbltournaments
SET prize_funding = CASE
    WHEN prize_type = 'AUTOMATIC' AND entry_fee_credits > 0 THEN 'ENTRY_FEES'
    ELSE 'FIXED'
END;
