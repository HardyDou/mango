# 能力说明维护规范

## 1. 定位

- 本文件约束 Mango 能力说明、模块 README、前端关键入口 README 和能力地图的维护。
- `mango-pmo` 仍是唯一长期规范源。
- 能力地图只做能力索引，模块 README 和前端关键入口 README 只做使用说明，不得复制长期规则正文。

## 2. 必须更新

以下变更必须同步判断并更新能力说明：

- 新增、删除或改变模块公开能力。
- 新增、删除或改变前端公开组件、页面注册入口、业务接入组件、运行时扩展点或跨包 API。
- 改变 API、配置项、注解、事件、菜单、权限、租户、数据源或初始化数据。
- 改变 CLI、starter、模板、发布物料或业务项目接入方式。
- 改变模块验收方式、启动方式、部署方式或升级步骤。
- 改变 E2E 覆盖范围、测试数据准备方式、验收证据格式或测试结果基线。
- 新增对业务开发者有影响的限制、常见错误或兼容性要求。

## 2.1 AI 能力刷新

Agent 处理 Mango 升级、业务接入、初始化数据、字典、菜单、角色、权限、工作流、Flyway、starter、模板、CLI 或发布物料相关任务时，必须先按当前分支的能力地图和模块 README 刷新事实源，再使用历史上下文、历史设计或历史 migration。

必须执行：

1. 先读当前分支的 `mango-docs/capabilities/README.md`，确认近期能力变更和组合阅读顺序。
2. 再读涉及模块的 README，确认当前推荐入口、配置、初始化方式和验证方式。
3. 发现历史 SQL、历史设计、旧 PR 记录、旧会话上下文与当前 README 或能力地图冲突时，以当前 README、能力地图和明确标注为当前口径的设计为准。
4. 历史 migration 只能作为数据库升级历史和兼容性证据，不能直接作为新增字典、菜单、角色、demo、测试数据或业务 seed 的当前实现模板。
5. 如果当前能力文档缺少新特性、升级步骤、初始化边界或验证入口，必须先补能力说明或登记文档缺口，不能继续按旧路径实现。

## 3. 更新位置

- 模块具体用法更新到对应模块 `README.md`。
- 前端复用组件、页面注册入口、业务接入组件或运行时扩展点的具体用法更新到对应入口目录 `README.md`。
- 能力索引更新到 `mango-docs/capabilities/README.md`。
- 长期规则更新到 `mango-pmo/rules/**`，并同步 `mango-pmo/rules/index.json`。
- 历史方案、交付事实和证据保留在 `mango-docs/designs/**`、`mango-docs/plans/**` 或 `mango-docs/evidence/**`。

## 4. 可不更新

以下变更通常不要求更新能力说明，但交付报告或 PR 必须说明原因：

- 纯内部重构，外部行为不变。
- 测试补充，不改变使用方式和验收口径。
- 局部缺陷修复，README 已覆盖正确行为。
- 注释、格式化、内部命名调整。
- 前端内部私有组件、页面局部拆分组件或样式文件调整，且不改变公开导出、页面 key、props、事件、API、权限、租户或验收方式。

## 5. 交付要求

正式交付和 PR 必须说明：

- 涉及的 Mango 能力。
- 模块 README 是否更新；未更新时说明原因。
- 前端关键入口 README 是否更新；未更新时说明原因。
- 能力地图是否更新；未更新时说明原因。
- PMO 规则是否更新；新增或修改规则时必须同步 `rules/index.json`。
- PR body 必须填写 PMO / Scope、Capability Docs、Validation 和 PMO Exceptions，不得保留模板占位。

## 5.1 发布前能力文档门禁

正式提交 PR 前必须完成以下检查：

- 改 `mango-pmo/rules/**` 时，必须同步 `mango-pmo/rules/index.json`，即使只是调整已有规则含义。
- 改 CLI、starter、模板、package 发布清单或业务接入方式时，必须判断相关业务集成指南是否受影响；无运行时影响也必须在对应指南写明公开 API、配置、菜单、权限、租户、页面、启动、验收或运行时行为不变。
- 模板 README 中指向生成后项目内文件的路径，优先写成代码路径；只有检查器能在模板源码位置解析到目标文件时才写 Markdown 相对链接。
- npm 包需要业务开发者离线阅读包能力说明时，必须把 package 根 `README.md` 纳入发布物料；Maven 运行时 jar 不作为 README 阅读入口，业务开发文档入口应落到文档站、版本匹配文档快照或生成模块 README。
- PR body 中的能力文档说明必须覆盖受影响能力、模块 README、能力地图、业务指南、PMO 规则和 `mango-pmo/rules/index.json`。
- 对外能力变更时，PR body 必须说明 E2E 脚本和测试结果基线是否更新；未更新时说明不适用原因或交付台账中的 `EXCEPTION` 依据。

发布前必须执行：

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
PR_BODY_FILE=/path/to/pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD
```

## 5.2 README 验收门禁

模块 README 必须让业务开发者能直接判断：

- 模块是什么、支持哪些能力、适合什么场景。
- 如何引入依赖、调用 API、注册前端页面或使用组件。
- 配置在哪里设置，支持哪些字段，默认值是什么，改动后影响什么行为。
- 数据库 migration、默认数据、字典、编号规则、任务、存储配置等由哪里初始化，幂等键是什么，在哪里确认生效。
- 菜单如何入库，菜单 `component` 对应哪个前端页面 key，按钮或接口权限码有哪些，默认套餐或角色如何获得授权。
- 租户、数据权限、登录态或业务归属由哪个后端入口校验。
- 页面入口、接口入口、常见排障路径是什么。
- 业务模块复用 Mango 能力时，能从 README 进入能力地图或对应模块 README。

前端入口 README 分两档：

- 纯管理页面入口可以轻量说明具体管理能力、页面 key、依赖接口、菜单和权限关系。
- 公共组件、公开 API、页面注册入口和运行时扩展点必须详细说明参数、默认值、事件、slot、示例、后端依赖、权限边界和常见排障入口。

README 验收必须执行：

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
```

## 6. 禁止事项

- 禁止在能力地图、README、设计文档或交付记录中新增长期“必须 / 禁止”规则。
- 禁止把能力地图写成第二套规范源。
- 禁止把模块 README 写成 PMO 规则文件。
- 禁止把前端入口 README 写成 PMO 规则文件。
- 禁止改公开能力后不说明能力文档影响。
