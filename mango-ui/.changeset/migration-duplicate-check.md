---
'@mango/pmo': patch
---

新增迁移重复建表检查 `check-migration-duplicates.mjs`：禁止同一迁移模块内两个文件创建同名表，已存在的历史重复登记在 `mango-pmo/baselines/migration-create-table-duplicates.txt`。
