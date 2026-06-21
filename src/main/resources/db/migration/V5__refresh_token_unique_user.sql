-- Garante um refresh token por usuário e remove duplicatas existentes
DELETE t1 FROM tblarena_refresh_tokens t1
INNER JOIN tblarena_refresh_tokens t2
    ON t1.user_type = t2.user_type
   AND t1.user_id = t2.user_id
   AND t1.id < t2.id;

CREATE UNIQUE INDEX uk_arena_refresh_user ON tblarena_refresh_tokens(user_type, user_id);
