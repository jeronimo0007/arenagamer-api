-- Um contact só pode pertencer a um time (dono ou membro).
-- Remove memberships duplicadas, mantendo a mais antiga por contact.
DELETE tm FROM tblteam_members tm
INNER JOIN (
    SELECT contact_id, MIN(id) AS keep_member_id
    FROM tblteam_members
    GROUP BY contact_id
) keeper ON tm.contact_id = keeper.contact_id AND tm.id <> keeper.keep_member_id;

-- Remove times órfãos (sem membros) ou times duplicados do mesmo dono (mantém o mais antigo).
DELETE t FROM tblteams t
INNER JOIN (
    SELECT owner_contact_id, MIN(id) AS keep_team_id
    FROM tblteams
    WHERE active = 1
    GROUP BY owner_contact_id
    HAVING COUNT(*) > 1
) keeper ON t.owner_contact_id = keeper.owner_contact_id AND t.id <> keeper.keep_team_id;

ALTER TABLE tblteam_members
    ADD UNIQUE KEY uk_team_member_contact (contact_id);
