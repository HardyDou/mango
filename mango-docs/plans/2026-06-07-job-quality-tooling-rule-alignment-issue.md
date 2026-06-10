# Job 静态检查规则对齐 Issue

## 1. 问题

Mango Job 模块执行 `mvn ... pmd:check` 时，P3C 规则 `ServiceOrDaoClassShouldEndWithImplRule` 要求服务实现类以 `Impl` 结尾；当前 `mango-pmo/rules/backend/02-naming.md` 明确约定内部服务实现命名为 `XxxService`。

同时，执行全仓 `mvn mango:check -Drule=all` 会扫描大量非 Job 模块的既有规则债务，并在本 worktree 报告 19672 个 issue。代表性类别包括：

- `*_core` 依赖 `*-starter` 的层级规则冲突，例如 `mango-infra-kv-core`、`mango-workflow-core`、`mango-notice-core`、`mango-system-core`、`mango-authorization-core`、`mango-job-core` 等。
- 多个 starter 缺少或不符合 `META-INF/mango/module.properties` 的 `module-path` 规则。
- Realtime、Org 等远程适配器路径与 `module-path` 规则不一致。
- Notice、Authorization、Workflow、Org、Calendar 等 API 契约不满足单参数 Query/Command 规则。
- 历史 migration 缺少统一审计字段，例如 authorization、calendar、numgen 等模块的 V1 脚本。

## 2. 影响

- `MangoJobDefinitionService`
- `MangoJobQueryService`
- `MangoJobWorkerRegistryService`
- `MangoJobEngineSyncService`
- `MangoJobAlarmRuleService`

这些类均符合当前 PMO 命名规范，但会被 P3C PMD 规则报告。

全仓 `mango:check` 的非 Job 命中项会导致“整仓质量门禁”失败，但大部分不属于本次 Mango Job 原生引擎交付范围；如果在当前任务内直接修复，会违反“不扩大 infra/common/其它模块范围”的任务约束。

## 3. 本次处理

本次 Mango 原生 Job 主任务不批量改成 `XxxServiceImpl`，避免为了工具规则扩大 Job 代码和引用改动范围。

本次不修改 infra、notice、workflow、system、authorization 历史模块以清空全仓 `mango:check`。Job 任务仅记录该治理问题，并继续以 Job 相关 Maven 测试、Job 聚合静态检查、前端构建、E2E 和交付台账作为本轮可控质量门禁。

## 4. 建议

由 PMO/质量工具任务统一裁决：

- 如果保留 PMO 的 `XxxService` 规范，则在 Mango 质量规则中屏蔽或降级该 P3C 规则。
- 如果改为 P3C `XxxServiceImpl` 规范，则统一更新 PMO 命名规范，并按模块分批迁移。
- 对 `mango:check -Drule=all` 增加“任务影响路径过滤”或“基线差异模式”，避免本次模块交付被全仓历史债务阻断。
- 对 `core -> starter` 依赖规则进行架构裁决：若 `mango-infra-persistence-starter` 等 starter 当前允许被 core 依赖，应更新 PMO/检查器规则；若不允许，应拆分 API/Support 后按模块迁移。
- 历史 migration 审计字段、module.properties、远程路径和 API 单参数规则应拆成独立治理 Sprint，逐模块处理。

## 5. 证据

命令：

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-support,mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter-remote,mango-platform/mango-job/mango-job-starter -am checkstyle:check pmd:check -DskipTests
```

结果：Maven Reactor `BUILD SUCCESS`，但 Job core 的 PMD 报告包含上述 5 条命名规则项。

命令：

```bash
cd mango
mvn mango:check -Drule=all
```

结果：Maven Reactor 在根模块 `mango` 失败，`Check failed: 19672 issue(s) found`。该失败包含全仓历史模块规则命中，并非本轮 Job 单模块验证失败；本轮不扩大范围修复。
