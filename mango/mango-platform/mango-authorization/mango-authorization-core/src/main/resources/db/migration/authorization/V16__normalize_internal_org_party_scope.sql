-- INTERNAL_ORG represents the tenant enterprise by default.
UPDATE `authorization_subject_role`
SET `party_id` = `tenant_id`
WHERE `subject_type` = 'TENANT_MEMBER'
  AND `party_type` = 'INTERNAL_ORG';
