# Issue #252 验证结果摘要

原始构建、启动和 PMO 命令日志属于过程产物，按 Issue #888 从 evidence 移除；本摘要保留可复核命令、环境和结果。

| 类别 | 命令/检查 | 结果 |
|---|---|---|
| 后端测试 | `mvn -pl mango-platform/mango-auth/mango-auth-starter -am test` | PASS，退出码 0 |
| 前端构建 | `pnpm --filter @mango/notice build` | PASS，退出码 0 |
| 数据验证 | 查询 `biz_domain.domain_code='AUTH'` 与 resource registry ACTIVE 记录 | PASS，结果详见 `logs/db-auth-domain.txt` |
| UI/API | `/domain/domains/enabled-tree`、`/notice/business-types?domainCode=AUTH`、接收设置页 | PASS，结果详见 `logs/ui-auth-domain.json` 与截图 |
| PMO | `delivery-contract-check`、`acceptance-evidence-check` | PASS，退出码 0 |

环境、账号标识和截图索引保留在 `acceptance-evidence.md`；密码和 token 未记录。
