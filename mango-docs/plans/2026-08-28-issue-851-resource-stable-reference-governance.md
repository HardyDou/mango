# Issue #851 Resource 稳定引用治理记录

## 1. 元数据

- 状态：IMPLEMENTED_PENDING_PR
- 交付模式：FULL
- 需求影响：L3，新约束面向所有新增和重构的跨模块 Resource 关系，影响平台长期模块协作设计。
- 方案风险：L1，仅修改规范、能力地图和使用说明，不修改 Java API、运行时排序或数据库行为，整体回退即可恢复。
- 最终风险：L3，取需求影响与方案风险的较高值。
- 工作区：M01=REUSE；分支 `docs/resource-stable-reference-governance`。
- 关联范围：[Issue #851](https://github.com/HardyDou/mango/issues/851)；本任务不实现重置发布或增量发布运行时代码，不合并、不发布、不部署。

## 2. 治理决定

1. 跨模块 Resource 关系以固定 `resourceId`、领域 `code` / `bizCode` 或 `resourceType + bizKey` 表达。
2. 目标 Handler 按稳定身份幂等解析和写入；目标暂未出现时返回可重试结果并重入收敛，非法或不可能满足的引用永久失败。
3. 运行时生成的目标表主键和模块、声明文件、JAR、Handler 执行顺序不作为新 Resource 的发布合同。
4. `ResourceProvider.moduleDependencies()`、声明文件 `moduleDependencies` / `module-dependencies` 和 `ResourceHandler.dependsOnResourceTypes()` 保留存量兼容，不删除运行能力，但不建议新增使用。
5. 长期约束只维护在 `mango-pmo/rules/backend/05-module.md`；Resource README 只说明用法并链接规范源，能力地图只登记入口和影响。业务 Starter 中的 PMO baseline 由 `sync-pmo-baseline.mjs` 从规范源生成，不作为第二套人工维护规则。

## 3. 取舍与兼容性

- 继续新增拓扑依赖会把跨模块正确性绑定到打包和执行顺序，无法满足模块独立协调和幂等重入目标，因此不采用。
- 立即删除已有依赖 API 会破坏存量声明和当前 1.0.42 行为，因此保留兼容实现并渐进迁移。
- 本次不新增 pending-reference 表、Resource 级 DAG 或新的 revision 体系；具体 Handler 的稳定引用迁移由后续实现任务逐类型完成。

## 4. 验收映射

| ID | 验收项 | 验证入口 | 预期 |
| --- | --- | --- | --- |
| GOV-001 | 长期规则只有一个规范源 | `check-governance-intent.mjs`、人工差异复核 | PMO 规则承载强制约束，README 只解释用法并链接 |
| PROJ-001 | 业务 PMO baseline 与规范源一致 | `check-template.mjs` | 受管投影及摘要无漂移 |
| DOC-001 | Resource 使用说明和能力入口同步 | README、能力地图审计 | 文档入口完整且与源码事实不冲突 |
| COMP-001 | 存量依赖功能保持兼容 | 变更文件和 Git diff 复核 | 无 Java、API、migration 或配置改动 |
| EXT-001 | 方案与 Issue 状态可回读 | GitHub Issue 评论和状态回读 | #851 保持 OPEN，评论记录当前设计且不宣称已实现 |

## 5. 回退与剩余风险

- 合并前可整体撤销本次文档变更；合并后可通过普通 revert 恢复，不涉及数据修复。
- 现有 Handler 尚未统一实现稳定身份的可重试解析，本次规范不代表重置发布或 Resource 动态增量已经完成。
- 存量拓扑依赖仍可能继续影响执行顺序；后续迁移必须逐类型验证幂等、重试和最终收敛语义。
