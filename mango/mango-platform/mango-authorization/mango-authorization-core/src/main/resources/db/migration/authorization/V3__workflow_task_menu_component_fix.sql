UPDATE `authorization_menu`
SET `component` = '@/views/workflow/task-list/index.vue',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` IN (260101,260102,260103,260104);

UPDATE `authorization_menu`
SET `component` = NULL,
    `redirect` = '/workflow/task/todo',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2601;
