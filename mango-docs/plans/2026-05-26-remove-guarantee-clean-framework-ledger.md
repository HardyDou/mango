# 删除保函业务清理台账

## 1. 目标

从 Mango 框架仓库中移除保函业务应用和业务实现，避免框架发布物携带具体业务系统代码。

## 2. 范围

- 删除 `mango-platform/mango-guarantee` 业务模块。
- 移除根 POM、平台 POM、单体应用对 `mango-guarantee` 的装配引用。
- 移除工作流初始化中的保函业务节点模板和转换器专用逻辑。
- 移除系统初始化数据中明确属于保函业务接入的能力字典。
- 移除框架 README 中保函业务示例文字。

## 3. 不做

- 不删除通用机构类型、金融机构、担保机构等平台基础字典。
- 不处理历史数据库中已执行过的保函表和数据。
- 不处理 `mango-docs` 中历史设计和计划文档。
- 不改文件预览相关未提交改动。

## 4. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CLEAN-GUARANTEE-001 | 用户要求 | 删除保函 app，确保框架干净 | 删除 `mango-guarantee` 三层模块，框架不再携带保函业务实现 | `mango/mango-platform/mango-guarantee` 删除 | `find` / `rg` 扫描 | DONE | `mango/mango-platform/pom.xml` |
| CLEAN-GUARANTEE-002 | 用户要求 | 框架构建不再包含保函模块 | 移除 reactor 和依赖管理中的 guarantee artifacts | `mango/pom.xml`、`mango/mango-platform/pom.xml` | Maven 编译 | DONE | `mango/pom.xml` |
| CLEAN-GUARANTEE-003 | 用户要求 | 应用装配不再启用保函模块 | 移除单体 app starter 依赖、Flyway 模块、OpenAPI 分组 | `mango/mango-app/monolith/mango-monolith-app/pom.xml`、`application.yml` | 引用扫描、Maven 编译 | DONE | `mango/mango-app/monolith/mango-monolith-app/pom.xml` |
| CLEAN-GUARANTEE-004 | 框架干净 | 工作流不携带保函业务节点模板 | 删除 `GUARANTEE_*` 初始化节点和转换器分支 | workflow core/starter | `rg "GUARANTEE_|保函"` | DONE | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/resources/db/migration/workflow/V1__init_workflow.sql` |
| CLEAN-GUARANTEE-005 | 框架干净 | 初始化数据不出现保函业务能力 | 仅移除明确业务能力，保留通用机构类型 | system migration | `rg "GUARANTEE_BUSINESS|BANK_COLLABORATION"` | DONE | `mango/mango-platform/mango-system/mango-system-core/src/main/resources/db/migration/system/V1__init_system.sql` |

## 5. 风险与限制

- 已执行过旧 migration 的本地或测试库不会自动删除历史保函表和历史字典，需要单独数据清理脚本。
- `mango-docs` 中历史保函设计资料保留，作为历史资料不参与框架发布。
