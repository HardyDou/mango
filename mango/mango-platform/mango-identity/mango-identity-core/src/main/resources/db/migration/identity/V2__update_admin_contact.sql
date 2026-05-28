UPDATE `identity_user`
SET `email` = '1012404303@qq.com',
    `phone` = '18701445644',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 1
  AND `username` = 'admin';
