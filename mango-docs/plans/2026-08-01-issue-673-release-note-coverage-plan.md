# Issue 673 PMO required-check 发布说明覆盖实施计划

## 1. 基线

- 设计：`mango-docs/designs/2026-08-01-issue-673-release-note-coverage.md`
- Issue：`https://github.com/HardyDou/mango/issues/673`
- 最终风险：L3
- 模式：FULL 治理
- 分支：`fix/issue-673-release-notes`

## 2. 实施清单

| ID       | 来源           | 交付项                                     | 验证                             | 状态 |
| -------- | -------------- | ------------------------------------------ | -------------------------------- | ---- |
| TASK-001 | AC-001         | 补全根与 CLI 1.0.94 发布说明               | 文本检查、release-notes 实际命令 | DONE |
| TASK-002 | AC-001         | 新增 PMO 1.3.8 独立 changelog              | 文本检查、版本段解析测试         | DONE |
| TASK-003 | AC-002、AC-003 | 增强 CLI 锁定 PMO required-check 覆盖门禁  | Node 定向测试                    | DONE |
| TASK-004 | AC-004、AC-005 | 执行实际发布说明检查和 PMO required checks | 命令结果与 diff 复核             | DONE |

## 3. 保障措施

- M01：REUSE；使用仓库外任务 worktree。
- M07：ENABLE；本设计与计划记录发布门禁治理决定。
- M08：ENABLE；更新三个消费/发布说明入口。
- M09：ENABLE；执行实际 release-notes、workspace 和样式检查。
- M10：ENABLE；覆盖版本段解析、checker 差异和缺项失败语义。
- M14：ENABLE；对门禁 fail-closed、历史 tag 选择和发布兼容性进行独立 diff 复核。
- M15：DISABLE；本次不写外部状态，Issue/PR/Release 更新留待独立授权。

## 4. 完成条件

- TASK-001 至 TASK-004 全部为 DONE。
- 所有定向验证退出码为 0。
- 不修改 PMO/CLI 版本和锁文件，不执行不可变发布动作。
- 未验证项和剩余风险在交付结果中明确记录。

## 5. 验证结果

- release-notes 单元测试：6/6 通过。
- `@mango/pmo@1.3.8` 与 `@mango/cli@1.0.94` 实际发布说明检查通过。
- admin 样式聚合、模块样式治理、release impact、workspace layout、Prettier 和
  `git diff --check` 全部通过。
- 本地 Node `26.5.0` 不在仓库声明的 `>=22.23.1 <23` 范围内；命令仅产生 engine
  warning，没有测试失败。CI 使用仓库约定的 Node 22。
