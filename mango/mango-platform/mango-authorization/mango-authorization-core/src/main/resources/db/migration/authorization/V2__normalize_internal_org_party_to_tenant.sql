DELETE duplicate_binding
FROM authorization_subject_role duplicate_binding
JOIN authorization_subject_role keeper
  ON keeper.subject_type = duplicate_binding.subject_type
 AND keeper.subject_id = duplicate_binding.subject_id
 AND keeper.role_id = duplicate_binding.role_id
 AND keeper.tenant_id = duplicate_binding.tenant_id
 AND keeper.app_code <=> duplicate_binding.app_code
 AND keeper.party_type = duplicate_binding.party_type
 AND keeper.id < duplicate_binding.id
WHERE duplicate_binding.party_type = 'INTERNAL_ORG';

UPDATE authorization_subject_role
SET party_id = tenant_id
WHERE party_type = 'INTERNAL_ORG'
  AND (party_id IS NULL OR party_id <> tenant_id);
