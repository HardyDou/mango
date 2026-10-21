# 标准交付记录

Issue #1053 CMS 公开站点交付缺口。

## 0. 使用合同与最小填写方式

填写人：本次 Dev/QA 负责人。事实来源：Issue #1053、当前 `origin/main` 源码、Maven 与 pnpm 命令输出、前端/后端测试报告。下游使用方：Review、发布负责人。

## 1. 元数据

- 任务 ID：Issue #1053（MMA-1053）
- 交付模式：STANDARD
- 需求影响：L2 - 公开 CMS 接口契约新增 `code`、`mediaUrl`、`top`、`sort` 字段并支持按编码读取；管理端新增 Banner 页面与 `cms:banner:*` 权限码
- 方案风险：L2 - 涉及 `cms_content` 新增列与租户内唯一键迁移、共享 `PageQuery` 别名、前端页面注册；均可通过回滚迁移与还原文件恢复
- 最终风险：L2
- 工作区决策：REUSE

## 2. 目标与范围

- 目标：消除 Issue #1053 中仍有真实消费方的缺口（发布栏目按站点过滤、演示内容分类语义、公开内容置顶排序、分页参数契约），并彻底删除已被广告投放取代的 Banner 能力。
- 成功条件：纳入范围的子项在源码中有对应实现与测试；`mvn verify` 与 `pnpm -F @mango/cms test` 通过；`mango:check` 的 `module-menu`、`resource-registry` 无新增违规。
- 处理范围：`mango/mango-platform/mango-cms/**`、`mango/mango-common` 的 `PageQuery`、`mango-ui/packages/cms`、`mango-ui/packages/site-shell`、CMS README 与 Changeset。
- 不处理范围：内容 `code`/`slug` 编码（无消费方）、`@mango/cms` 组件级 E2E、业务项目升级实测。
- 破坏性范围：`V2__drop_cms_banner.sql` 删除 `cms_banner` 表，历史 Banner 数据不可恢复；升级前需按 README 12.1 清理 `resource_registry` 历史 `CMS_BANNER` 行。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 内容运营 → 发布管理 → 新增发布 | 站点选择「元丰行担保」 | 「发布栏目」下拉只出现该站点栏目；切换站点立即重载 | 未选站点时下拉为空，加载失败给出错误提示 | 前端配置与组件按 `siteId` 调用 `treeSiteCategories` |
| R2 | 内容运营 → 内容中心 → 编辑内容 | 平台 demo 内容 | 「内容分类」显示真实分类名称，不再是雪花 ID | 分类缺失时显示占位 | demo 声明包含 `CMS_CONTENT_CATEGORY`，内容 `categoryId` 指向内容分类 |
| R7 | 直连 HTTP 的消费方 | 传 `pageNum=1&pageSize=50` | 返回 50 条，与 `page=1&size=50` 等价 | 非法分页仍按既有校验拒绝 | `PageQuery` 别名映射 + 单测 |
| R8 | 公开站点 → `GET /cms/open/contents/page` | 同一内容在多个子栏目并设置置顶 | 返回 `top`、`sort`，跨栏目合并可还原后台顺序 | 无发布关系时字段为空 | `SiteContentVO.top/sort` 由发布关系填充 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1 | 前端按 `form.siteId` 加载栏目树，不再遍历全部站点拼接 | `CmsResourceView.vue` | 还原文件 |
| D2 | R2 | 新增 `CMS_CONTENT_CATEGORY` demo 声明，内容 `categoryId` 改指内容分类；发布关系继续指向站点栏目 | demo 声明文件 | 还原声明文件 |
| D6 | R7 | 共享 `PageQuery` 接受 `pageNum`/`pageSize` 别名，不改变既有 `page`/`size` 语义 | `mango-common` | 还原文件 |
| D8 | R8 | `top`、`sort` 取自 `cms_content_publish`，保持 SQL 既有排序 | `CmsSiteService` | 还原方法 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1 | 1 | `mango-ui/packages/cms/src/views/CmsResourceView.vue` | 按站点加载栏目树 |
| I2 | D2 | 2 | `mango-cms-starter/src/main/resources/META-INF/mango/demo/**` | 新增 3 条内容分类，74 条 demo 资源 |
| I4 | D8 | 4 | `SiteContentVO`、`CmsSiteService` | 置顶与排序透出 |
| I6 | D6 | 6 | `mango-common/src/main/java/io/mango/common/po/PageQuery.java` | 别名映射就绪 |
| I8 | D3 | 8 | `cms_banner` 全套文件、`V2__drop_cms_banner.sql`、`site-shell`、演示站点 App、演示广告数据 | Banner 能力彻底删除且演示站点改用广告投放 |
| I7 | 全部 | 7 | `mango-cms/README.md`、`mango-ui/.changeset/issue-1053-cms-public-site-gaps.md` | 文档与发布说明同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1 | 前端配置测试 | `pnpm -F @mango/cms test` | PASS | `src/__tests__/cmsResourceConfigs.spec.ts` |
| R2 | 后端声明契约测试 | `mvn -pl ...mango-cms-starter test`（`CmsResourceDeclarationContractTest`） | PASS | demo 资源数 74、类型含 `CONTENT_CATEGORY` |
| R7 | 单元测试 | `PageQueryTest`（7 tests） | PASS | 别名映射、上限钳制、空值忽略 |
| R8 | 后端行为测试 | `CmsSiteServiceBehaviorTest` | PASS | `top`/`sort` 断言 |
| Banner 删除 | 迁移与契约测试 | `CmsMigrationContractTest#cmsDropsRemovedBannerTable`、`CmsApiSurfaceContractTest`、`CmsRemoteAdapterContractTest`、`CmsPublicApiDeclarationContractTest` | PASS | V1 无 `cms_banner`、V2 执行 `DROP TABLE`、管理 API 60 个方法、公开声明 7 条 |
| 全局 | 架构与静态门禁 | `mvn ... -Dmango.architecture.mode=changed verify` | PASS | `gateStatus=PASS` |
| 全局 | 模块规则 | `mango:check -Drule=module-menu`、`-Drule=resource-registry`（changedOnly + baseRef origin/main） | PASS | 新增违规 0 |
| 全局 | 前端构建 | `pnpm -F @mango/cms build`、`pnpm -F @mango/site-shell build` | PASS | dist 产物生成 |
| 全局 | 样式治理 | `pnpm admin:styles:check`、`pnpm admin:module-styles:check` | PASS | 20 个样式导出一致、14 个官方模块通过 |

## 7. 例外与剩余风险

- 未执行浏览器运行态验收：发布栏目下拉的交互由前端配置测试覆盖，未启动真实后端与浏览器复验。
- 未执行业务项目消费实测：公开站点字段与分页别名由后端测试覆盖，未在 baohan-system 升级后回读。
- 未在已有数据的老库执行 `V2__drop_cms_banner.sql`：删表不可恢复，升级前必须先完成 README 12.1 的注册行清理与可选归档。
- 内容 `code`/`slug` 与 Banner 管理菜单未实现：前者无消费方，后者与 2026-07-14 的 `V4__remove_cms_banner_menu.sql` 决定和站点 App 的 `banners/list` 反向断言冲突，按最小实现原则不加。
- M02（不重建数据库）、M03/M04/M05/M06（无独立需求/设计/计划文档）、M07（无治理规则变更）、M13（无浏览器 UI 验收）、M14（未发起外部同行评审）、M16（未做人工环境验收）保持关闭，理由均为本次为已登记 Issue 的定点修复，证据以自动化测试与门禁为主。
