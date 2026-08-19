UPDATE identity_external_binding binding
JOIN identity_user user_account ON user_account.id = binding.user_id
SET binding.display_name = NULL
WHERE binding.provider = 'WECOM'
  AND binding.bind_source = 'SELF'
  AND binding.display_name = user_account.nickname;
