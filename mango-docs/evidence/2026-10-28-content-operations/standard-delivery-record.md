# 标准交付记录

> 内容运营问题

## 1. 元数据

- 任务 ID：content-operations
- 交付模式：STANDARD
- 需求影响：L2 - 影响 CMS 内容分类初始化、内容详情展示、内容审核到发布的流程、发布记录语义和发布配置。
- 方案风险：L2 - 同时修改 CMS 管理前端、发布记录表结构与唯一约束、内容下线联动、Resource 能力说明和浏览器 E2E。
- 最终风险：L2
- 工作区决策：CREATE；任务分支 `fix/content-operations`，工作区 `/Users/hardy/Work/mango-content-operations`
- 评审选择：`AGENT_ONLY`。用户未指定外部同行评审，按规则保留三视角分析，不将其称为人工评审。

## 2. 目标与范围

- 目标：修复内容运营的四类问题，并按精简方案调整发布入口和职责。
- 成功条件：
  1. 业务可以用 `CMS_CONTENT_CATEGORY` 声明正式内置内容分类。
  2. 内容详情弹窗可以解析并显示富文本托管图片。
  3. 新增内容打开编辑器时重新加载内容分类，不需要刷新页面。
  4. 置顶范围、推荐类型显示业务标签；编辑发布记录可以回显原发布内容和栏目。
  5. 内容中心是发布主入口：一次发布选择一个站点和多个栏目，可重复发布到其他站点。
  6. 「发布管理」改名为「发布记录」，只维护已有记录，不提供新建入口。
  7. 发布、下线、再发布产生多条记录，历史保留；同一目标只保留一条生效记录。
  8. 内容下线时连带下线该内容所有生效发布记录；已下线内容可重新编辑和提交审核。
  9. 删除内容时同时删除其发布记录和标签关系，不留下孤儿记录。
- 处理范围：CMS 管理前端 `@mango/cms`、`@mango/cms` 浏览器 E2E、CMS core 发布与内容服务、CMS 菜单资源、CMS migration、CMS Resource 处理器契约测试、CMS 与 Resource README。
- 不处理范围：不新增平台 Demo 数据；不把运行时用户创建的分类迁移为 Resource；发布记录不保存内容快照；推荐位置保持单值。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-01 | 业务 Resource 声明 | CMS starter 已装配；正式资源目录；稳定 targetId | `CMS_CONTENT_CATEGORY` 同步到 `cms_content_category` | 缺少必填字段或目标 handler 时同步失败 | handler spec 和 Maven 测试通过 |
| AC-02 | 内容中心详情弹窗 | 内容正文包含普通文本和托管图片 | 图片显示，危险 HTML 不执行 | 文件无权限时显示资源不可用状态，不显示错误地址 | RichTextViewer 组件和浏览器链路通过 |
| AC-03 | 内容中心新增内容 | 先在内容分类页新增分类，再打开内容新增弹窗 | 分类选项重新加载并可选 | 分类请求失败显示错误反馈 | CMS E2E 内容分类/内容中心链路通过 |
| AC-04 | 内容发布编辑 | 发布记录已有 contentId、categoryId、topScope、recommendationType | 内容、栏目和枚举 label 正确回显 | 选项请求失败显示错误反馈，不伪造选项 | Vitest 和浏览器发布链路通过 |
| AC-05 | 内容中心发布入口 | 内容状态为已审核通过 | 操作列提供“发布”；一次发布选择一个站点和多个栏目 | 未选站点或栏目时提示校验失败 | 浏览器发布成功并生成发布关系 |
| AC-06 | 发布记录页面 | 已存在发布记录 | 菜单名为「发布记录」；仅提供编辑、下线、删除，不提供新建入口 | 直接访问页面不出现“新增” | Playwright 断言菜单名与无“新增”按钮 |
| AC-07 | 发布历史 | 同一内容、同一站点栏目反复发布 | 发布、下线、再发布产生多条记录，仅最新一条生效 | 重复发布不覆盖历史 | API 实测记录条数与状态 |
| AC-08 | 内容下线联动 | 内容存在生效发布记录 | 内容下线时，所有生效发布记录同步置为已下线 | 无生效记录时不报错 | API 实测生效记录数为 0 |
| AC-09 | 下线后重新提交 | 内容状态为已下线 | 允许编辑并重新提交审核 | 其他非法状态仍拒绝 | 单元测试覆盖 |
| AC-10 | 删除内容 | 内容存在发布记录和标签关系 | 删除内容时一并删除发布记录和标签关系 | 事务回滚保证一致 | 单元测试验证删除调用 |

### 三视角复核

| 视角 | 当前来源 | 结论与依据 | 未解决项 | 下一步输入 |
|---|---|---|---|---|
| 事实与用户视角 | CMS README、CMS 页面配置、浏览器 E2E | Resource handler 已存在但能力清单和使用说明遗漏；详情使用普通 `v-html`，不能解析 `mango-file:` 图片；编辑器选项只在页面初始化加载；发布编辑行字段与编辑表单字段不一致。 | 富文本图片需要真实文件权限才能完整观察。 | 使用 `RichTextViewer` 和真实 CMS E2E 回归。 |
| 技术与风险视角 | `CmsResourceHandlerConfiguration`、`CmsTableResourceHandler`、`CmsResourceView`、`configs.ts` | 复用已有 CMS handler、`RichTextViewer` 和枚举选项映射；不新增 API 或数据库字段。发布记录通过现有 `contentId/categoryId` 转换为表单数组。 | Resource 目标应用仍必须装配 `mango-cms-starter`。 | 增加 handler spec 契约测试和前端配置测试。 |
| 验证与交付视角 | CMS Vitest、CMS Maven 测试、Playwright CMS E2E、README 审计 | 真实前后端启动成功；CMS P0/P2 浏览器用例通过；模块 README 审计存在一个与本任务无关的既有 DictSelect 文档缺口。 | 未单独保存成功页面截图；保留 Playwright JSON 报告。 | 交付报告标记该文档审计剩余风险。 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | 分类 Resource | 使用已有 `CMS_CONTENT_CATEGORY` handler；业务正式资源放 `META-INF/mango/resources/`，使用 `SYNC_ONCE`、稳定 `targetId` 和 `parentId=0` 根分类。 | `mango-cms-starter`、CMS README、Resource README | 删除新增文档和契约测试；不涉及运行时表结构。 |
| D-02 | 详情图片 | 详情弹窗统一复用 `@mango/common` 的 `RichTextViewer`，让其按文件 ID 获取真实预览地址并继续执行富文本清理。 | `mango-ui/packages/cms/src/views/CmsResourceView.vue` | 恢复原详情渲染实现。 |
| D-03 | 分类即时刷新 | 打开编辑器时重新加载关联选项；不使用静态分类数据。 | `CmsResourceView.vue` | 删除打开编辑器时的刷新调用。 |
| D-04 | 发布字段与编辑 | 置顶范围、推荐类型使用枚举 value/label 映射；发布记录单值 ID 转为单值表单字段，发布时再组装为接口要求的数组。 | `configs.ts`、`CmsResourceView.vue` | 恢复原配置和行回显逻辑。 |
| D-05 | 发布入口分层 | 内容中心负责发起发布：一次一个站点 + 多个栏目，可重复发布到其他站点；「发布记录」页面只维护已有记录，不新增关系。 | `configs.ts`、`CmsResourceView.vue`、CMS E2E | 恢复「发布记录」新增按钮和原发布配置。 |
| D-06 | 发布历史 | 移除 `cms_content_publish` 的 `uk_cms_publish_position` 唯一约束，改为普通索引；重新发布时把旧生效记录置为已下线并插入新记录。 | `V3__cms_publish_history.sql`、`CmsContentPublishService` | 删除 V3 migration 并恢复原 upsert 逻辑（会丢失历史记录）。 |
| D-07 | 下线与删除联动 | 内容下线时批量下线该内容生效发布记录；删除内容时删除其发布记录和标签关系，均在同一事务内。 | `CmsContentService` | 移除联动调用。 |
| D-08 | 发布弹窗结构 | 表单分「发布目标／展示设置／发布安排」三组；置顶范围、推荐位置、发布方式、有效期使用单选；站点未选时禁用发布栏目。 | `CmsResourceView.vue`、`@mango/cms` style.css | 恢复原弹窗布局。 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 | 1 | `mango-cms/README.md`、`mango-resource/README.md`、`CmsResourceHandlerConfigurationTest.java` | 文档列出 CMS 资源类型、正式分类声明示例，handler spec 测试通过。 |
| I-02 | D-02 | 2 | `mango-ui/packages/cms/src/views/CmsResourceView.vue` | 详情使用 `RichTextViewer`，不再使用页面私有富文本解析。 |
| I-03 | D-03 | 3 | `CmsResourceView.vue` | 打开新增/编辑弹窗重新请求关联分类。 |
| I-04 | D-04 | 4 | `mango-ui/packages/cms/src/views/configs.ts`、`cmsResourceConfigs.spec.ts` | 发布字段使用 label，发布记录编辑回显内容和栏目。 |
| I-05 | D-05 | 5 | `mango-ui/packages/cms/src/views/CmsResourceView.vue`、`mango-ui/packages/cms/src/views/configs.ts` | 内容中心提供发布弹窗（单站点 + 多栏目）、发布渠道查看；发布管理不再提供新增关系入口。 |
| I-06 | D-05 | 6 | `mango-ui/apps/mango-admin-shell/e2e/specs/cms-operations-buttons.spec.ts`、`cms-management.spec.ts` | 浏览器用例改为从内容中心发布，并断言发布记录页无新增入口。 |
| I-07 | D-06 | 7 | `V3__cms_publish_history.sql`、`CmsContentPublishService.java`、`CmsAdminServiceSecurityTest.java`、`CmsMigrationContractTest.java` | 发布/下线/再发布产生多条记录，单测与 API 实测通过。 |
| I-08 | D-07 | 8 | `CmsContentService.java`、`CmsAdminServiceSecurityTest.java` | 下线联动与删除级联有单元测试覆盖。 |
| I-09 | D-08 | 9 | `CmsResourceView.vue`、`style.css`、`cms-common-menu.json` | 弹窗分三组、控件为单选、菜单改名发布记录，浏览器实测通过。 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-01 | M09/M11 | `mvn -f mango/mango-platform/mango-cms/pom.xml -pl :mango-cms-starter -am test -DskipITs` | PASS；CMS core 34 项、starter 23 项测试通过。 | Maven 输出；`CmsResourceHandlerConfigurationTest` 通过。 |
| AC-02 | M09/M10 | `pnpm --filter @mango/admin-pages build && pnpm --filter @mango/http-client build && pnpm --filter @mango/cms test` | PASS；3 个测试文件、6 项测试通过。 | Vitest 输出。 |
| AC-03 | M09 | `pnpm --filter @mango/cms build` | PASS。 | Vite build 输出。 |
| AC-04 | M09 | Prettier check、ESLint、`git diff --check` | PASS；无新增 lint 错误。 | 命令输出。 |
| AC-05 | M12/M13 | 使用后端 `18005`、Admin Shell `31005`、CMS 子应用 `35005`、数据库 `mango_dev_mango_content_operations_005` 启动真实链路；运行 `cms-management.spec.ts --grep '@p0'` | PASS；1 项浏览器用例通过。 | `.runtime/playwright/mango-admin-shell/report.json`。 |
| AC-06 | M12/M13 | 运行 `cms-operations-buttons.spec.ts @p2` | PASS；含内容中心发布、发布记录下线，且断言发布记录页无新增入口。 | `.runtime/playwright/mango-admin-shell/report.json`。 |
| AC-07 | M08 | `node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS。 | README 源码事实审计输出。 |
| AC-08 | M08 | `node mango-pmo/tools/audit-module-readmes.mjs` | BLOCKED；发现既有 `mango-ui/packages/common/components/DictSelect/README.md` 缺少模块章节，与本任务路径无关。 | 审计输出；未扩大范围修复。 |
| AC-09 | M09/M13 | 启动真实前后端后打开 `/#/cms/contents`，对已审核通过内容点击“发布”，检查弹窗分组、控件和联动 | PASS；三组标题为发布目标／展示设置／发布安排；站点未选时发布栏目禁用，选站点后可用；无 console/network 错误。 | `mango-docs/evidence/2026-10-28-content-operations/publish-dialog.png`。 |
| AC-10 | M12/M13 | 在发布记录页打开已有记录“编辑”，检查回显并保存 | PASS；站点、内容、栏目、置顶、推荐正确回显；保存返回 200。 | `mango-docs/evidence/2026-10-28-content-operations/publish-record-edit.png`。 |
| AC-11 | M10/M12 | 同一内容同一目标执行发布→下线→再发布 | PASS；接口返回 3 条记录，1 条 PUBLISHED、2 条 OFFLINE。 | `CmsAdminServiceSecurityTest`；API 实测输出。 |
| AC-12 | M10/M12 | 内容存在生效发布记录时调用内容下线 | PASS；下线后生效记录数为 0，内容状态 OFFLINE；随后可重新提交为 PENDING_REVIEW。 | `CmsAdminServiceSecurityTest`；API 实测输出。 |
| AC-13 | M10 | 删除内容 | PASS；`deleteContent` 在同一事务内删除发布记录与标签关系。 | `CmsAdminServiceSecurityTest#deleteContent_removesPublishRelationsAndTagRelations`。 |

## 7. 例外与剩余风险

- 例外：模块 README 审计仍受既有 `DictSelect` README 缺口阻塞；本次任务保留真实 Playwright JSON 报告和发布弹窗、发布记录截图。
- 剩余风险一：`DictSelect` README 的既有文档审计缺口需另行治理，不阻断本任务代码验证。
- 剩余风险二：发布记录不保存内容快照，内容改名后历史记录标题会随之变化；用户已确认本轮不处理。
- 剩余风险三：推荐位置为单值，用户已确认本轮不处理。
- 环境说明：工作区数据库曾存在 `authorization_api_resource` 软删除残留导致启动唯一键冲突，已清理本地数据；该幂等缺陷属框架层，需另行治理。
- 服务已停止；工作区数据库 `mango_dev_mango_content_operations_005` 保留，未执行 workspace release。
