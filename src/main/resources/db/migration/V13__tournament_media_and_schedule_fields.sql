ALTER TABLE tbltournaments
    ADD COLUMN registration_opens_at DATETIME NULL AFTER registration_deadline,
    ADD COLUMN expected_end_date DATETIME NULL AFTER registration_opens_at,
    ADD COLUMN game_image_url VARCHAR(500) NULL AFTER expected_end_date,
    ADD COLUMN cover_image_url VARCHAR(500) NULL AFTER game_image_url,
    ADD COLUMN youtube_url VARCHAR(500) NULL AFTER cover_image_url,
    ADD COLUMN twitch_url VARCHAR(500) NULL AFTER youtube_url;
