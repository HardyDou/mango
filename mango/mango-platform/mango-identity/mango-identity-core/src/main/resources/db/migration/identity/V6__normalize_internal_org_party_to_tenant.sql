UPDATE identity_user
SET party_id = CAST(tenant_id AS UNSIGNED)
WHERE party_type = 'INTERNAL_ORG'
  AND tenant_id REGEXP '^[1-9][0-9]*$'
  AND CAST(tenant_id AS UNSIGNED) <= 9223372036854775807
  AND (party_id IS NULL OR party_id <> CAST(tenant_id AS UNSIGNED));
