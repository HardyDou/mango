# Issue #1017 Mango App 本地 main 启动实施计划

## 目标与范围

实现 IDEA 直接运行 Mango App `main` 时与 `mango dev start` 等价的本地启动流程；保留生产显式 Bootstrap/Runtime 生命周期。

## 风险与模式

- 需求影响：L2，影响本地启动、数据库迁移和运行时入口。
- 方案风险：L3，跨 Bootstrap、Persistence、App、CLI，涉及已有数据库和运行时生命周期。
- 最终风险：L3，交付模式 FULL。
- 工作区：CREATE，`../mango-main-local-startup`，分支 `feat/issue-1017-main-local-startup`。
- Issue：<https://github.com/HardyDou/mango/issues/1017>

## 交付项

| ID | 交付项 | 路径 | 完成标准 | 状态 |
|---|---|---|---|---|
| DEL-001 | 本地 main 双阶段启动 | `mango-infra-bootstrap-starter` | 无参数 main 自动 bootstrap 后启动 runtime；显式参数行为不变 | IMPLEMENTED |
| DEL-002 | 工作区配置与数据库边界 | `mango-infra-bootstrap-starter` | 读取 workspace env，复用已有库，只允许 `mango_dev_*` 自动建库 | IMPLEMENTED |
| DEL-003 | CLI/Java identity 合同 | `mango-ui/packages/mango-cli`、starter | 同一 workspace/app 使用稳定 environment/release tuple | IMPLEMENTED |
| DEL-004 | 回归测试 | starter、CLI tests、App | 空库、已有 generation、指纹变化和显式生命周期覆盖 | PARTIAL（空库、已有历史库、generation 复用、指纹变化、CLI 与实际 main 已验证；IDEA 图形化 Run/Debug 尚待执行） |
| DEL-005 | 使用说明 | bootstrap、app、CLI README | 说明 IDEA main、CLI、生产显式入口和失败边界 | IMPLEMENTED |

## 实施顺序

1. 抽取并固定本地 environment/release identity 合同。
2. 实现本地工作区配置加载和安全数据库准备。
3. 实现 Bootstrap context 到 Runtime context 的本地 main 编排。
4. 统一 CLI 的 identity 参数和 Java 本地入口。
5. 补充单元、集成和 CLI 合同测试。
6. 更新模块 README 和 Issue 记录。
7. 执行受影响 Maven 模块、CLI 测试和真实工作区启动验证。

## 验收映射

| ID | 验收标准 | 验证 |
|---|---|---|
| AC-001 | IDEA main 无手工生命周期参数启动 | starter 集成测试 + 真实 main/health |
| AC-002 | 已有历史库只执行必要增量迁移 | Bootstrap/Persistence 集成测试，检查模块 history 和 generation |
| AC-003 | 空库初始化并启动 | 本地工作区空库验证 |
| AC-004 | Bootstrap/Runtime 回执、指纹、lease 保持生效 | starter 生命周期测试和日志/数据库回读 |
| AC-005 | CLI 与 Java 使用同一 identity 合同 | CLI Node 测试和 Java 测试 |
| AC-006 | 显式 bootstrap/runtime 兼容 | `MangoApplicationTest` |

## 回退

如本地入口验证失败，保留显式 `bootstrap`/`runtime` 和现有 `mango dev start` 路径；不修改生产状态机和数据库结构。代码回退通过移除本地入口改动完成，不执行数据库 destructive rollback。

## 不确定项

- 真实工作区已有数据库的指纹和 migration 状态需要在验证阶段读取；禁止使用密码、token 或敏感数据写入记录。
- 微服务 App 的 CLI 清单需要按实际 manifest 逐个确认；不因单体验证通过而声明全部微服务已验证。
