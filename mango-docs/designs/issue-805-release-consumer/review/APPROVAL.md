# Issue #805 实施授权记录

- 需求源：https://github.com/HardyDou/mango/issues/805
- 授权人：HardyDou
- 授权时间：2026-08-16
- 授权内容：用户明确要求“开始处理 issue 805 的问题吧 处理”，同意按当前 Mango PMO 基线在专用 worktree 实施修复与本地验证。
- 范围追加：用户于 2026-08-16 明确要求“先解决循环依赖问题，让本次发布内容可用”，授权在同一 Issue #805 worktree 内拆除 `@mango/admin-pages -> @mango/system -> @mango/file -> @mango/admin-pages` 发布依赖环；发布质量预防机制另建 Issue，不纳入本次实现。
- 权限边界：不包含 Commit、Push、PR、合并、发布、关闭 Issue 或其它远端写操作。
