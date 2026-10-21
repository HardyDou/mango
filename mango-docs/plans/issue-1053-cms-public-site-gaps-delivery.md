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

- 目标：一次性消除 Issue #1053 中仍未解决的 7 个子项，公开站点不再需要业务侧手工绕行。
- 成功条件：7 个子项在源码中有对应实现与测试；`mvn verify` 与 `pnpm -F @mango/cms test` 通过；`mango:check` 的 `module-menu`、`resource-registry` 无新增违规。
- 处理范围：`mango/mango-platform/mango-cms/**`、`mango/mango-common` 的 `PageQuery`、`mango-ui/packages/cms`、`mango-ui/packages/site-shell`、CMS README 与 Changeset。
- 不处理范围：Banner 与广告投放的能力合并、`@mango/cms` 组件级 E2E、真实云端对象存储验收、业务项目升级实测。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 内容运营 → 发布管理 → 新增发布 | 站点选择「元丰行担保」 | 「发布栏目」下拉只出现该站点栏目；切换站点立即重载 | 未选站点时下拉为空，加载失败给出错误提示 | 前端配置与组件按 `siteId` 调用 `treeSiteCategories` |
| R2 | 内容运营 → 内容中心 → 编辑内容 | 平台 demo 内容 | 「内容分类」显示真实分类名称，不再是雪花 ID | 分类缺失时显示占位 | demo 声明包含 `CMS_CONTENT_CATEGORY`，内容 `categoryId` 指向内容分类 |
| R4 | admin 账号 → 内容运营 → 投放管理 | 已同步资源 | 出现「Banner 管理」菜单并可读写 | 无权限时按 `cms:banner:*` 拒绝 | 菜单声明含 6 个 `cms:banner:*` 且与 Controller `@ApiAccess` 一致 |
| R5 | 公开站点 → `GET /cms/open/banners/list` | 站点已有 Banner 或广告投放 | 返回可直接访问的 `mediaUrl` | 无媒体时 `mediaUrl` 为空 | `SiteBannerVO.mediaUrl` 由 `publicFileUrl` 生成 |
| R6 | 运营 → 内容编辑 | 填写内容编码 | 编码按租户唯一保存；公开接口可按编码读取 | 重复编码返回「内容编码已存在」 | `V2` 迁移 + `SELECT ... WHERE code` 查询 |
| R7 | 直连 HTTP 的消费方 | 传 `pageNum=1&pageSize=50` | 返回 50 条，与 `page=1&size=50` 等价 | 非法分页仍按既有校验拒绝 | `PageQuery` 别名映射 + 单测 |
| R8 | 公开站点 → `GET /cms/open/contents/page` | 同一内容在多个子栏目并设置置顶 | 返回 `top`、`sort`，跨栏目合并可还原后台顺序 | 无发布关系时字段为空 | `SiteContentVO.top/sort` 由发布关系填充 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1 | 前端按 `form.siteId` 加载栏目树，不再遍历全部站点拼接 | `CmsResourceView.vue` | 还原文件 |
| D2 | R2 | 新增 `CMS_CONTENT_CATEGORY` demo 声明，内容 `categoryId` 改指内容分类；发布关系继续指向站点栏目 | demo 声明文件 | 还原声明文件 |
| D3 | R4 | 采用「补齐菜单与权限」而非下线 Banner；权限码与现有 `@ApiAccess` 保持一致 | `cms-common-menu.json`、`@mango/cms` 页面 | 还原菜单声明与页面注册 |
| D4 | R5 | `mediaUrl` 由 `publicFileUrl(site, fileId)` 生成，复用 `public-preview` 地址规则 | `CmsSiteService` | 还原方法 |
| D5 | R6 | `cms_content.code` 可空，租户内唯一；服务层做重复校验 | `V2__add_content_code.sql`、`CmsContentService` | 新增列可保留，回滚仅需停止写入编码 |
| D6 | R7 | 共享 `PageQuery` 接受 `pageNum`/`pageSize` 别名，不改变既有 `page`/`size` 语义 | `mango-common` | 还原文件 |
| D8 | R8 | `top`、`sort` 取自 `cms_content_publish`，保持 SQL 既有排序 | `CmsSiteService` | 还原方法 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1 | 1 | `mango-ui/packages/cms/src/views/CmsResourceView.vue` | 按站点加载栏目树 |
| I2 | D2 | 2 | `mango-cms-starter/src/main/resources/META-INF/mango/demo/**` | 新增 3 条内容分类，74 条 demo 资源 |
| I3 | D3 | 3 | `cms-common-menu.json`、`admin-pages.ts`、`configs.ts`、`BannersView.vue` | Banner 菜单与页面可达 |
| I4 | D4、D8 | 4 | `SiteBannerVO`、`SiteContentVO`、`CmsSiteService` | 字段与实现就绪 |
| I5 | D5 | 5 | `V2__add_content_code.sql`、`CmsContentEntity`、`CmsContentService`、`SiteContentDetailQuery` | 迁移与查询就绪 |
| I6 | D6 | 6 | `mango-common/src/main/java/io/mango/common/po/PageQuery.java` | 别名映射就绪 |
| I7 | 全部 | 7 | `mango-cms/README.md`、`mango-ui/.changeset/issue-1053-cms-public-site-gaps.md` | 文档与发布说明同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1 | 前端配置测试 | `pnpm -F @mango/cms test` | PASS | `src/__tests__/cmsResourceConfigs.spec.ts` |
| R2 | 后端声明契约测试 | `mvn -pl ...mango-cms-starter test`（`CmsResourceDeclarationContractTest`） | PASS | demo 资源数 74、类型含 `CONTENT_CATEGORY` |
| R4 | 前端注册 + 配置测试 | `pnpm -F @mango/cms test`（`adminPages.spec.ts`、`cmsResourceConfigs.spec.ts`） | PASS | Banner 页面 loader 与 5 个 API 调用断言 |
| R5 | 后端行为测试 | `CmsSiteServiceBehaviorTest#listBanners_returnsDirectMediaUrlForPublicBanner` | PASS | cms-core 35 tests |
| R6 | 迁移契约 + 服务行为 | `CmsMigrationContractTest`、`CmsSiteServiceBehaviorTest` | PASS | V1 + V2 迁移清单、按编码查询 |
| R7 | 单元测试 | `PageQueryTest`（7 tests） | PASS | 别名映射、上限钳制、空值忽略 |
| R8 | 后端行为测试 | `CmsSiteServiceBehaviorTest` | PASS | `top`/`sort` 断言 |
| 全局 | 架构与静态门禁 | `mvn ... -Dmango.architecture.mode=changed verify` | PASS | `gateStatus=PASS` |
| 全局 | 模块规则 | `mango:check -Drule=module-menu`、`-Drule=resource-registry`（changedOnly + baseRef origin/main） | PASS | 新增违规 0 |
| 全局 | 前端构建 | `pnpm -F @mango/cms build`、`pnpm -F @mango/site-shell build` | PASS | dist 产物生成 |
| 全局 | 样式治理 | `pnpm admin:styles:check`、`pnpm admin:module-styles:check` | PASS | 20 个样式导出一致、14 个官方模块通过 |

## 7. 例外与剩余风险

- 未执行浏览器运行态验收：Banner 页面与发布栏目下拉的交互由前端配置/注册测试覆盖，未启动真实后端与浏览器复验。
- 未执行真实 MySQL 升级验收：`V2__add_content_code.sql` 仅通过迁移契约测试，未在已有数据的老库上执行。
- 未执行业务项目消费实测：公开站点字段与分页别名由后端测试覆盖，未在 baohan-system 升级后回读。
- M02（不重建数据库）、M03/M04/M05/M06（无独立需求/设计/计划文档）、M07（无治理规则变更）、M13（无浏览器 UI 验收）、M14（未发起外部同行评审）、M16（未做人工环境验收）保持关闭，理由均为本次为已登记 Issue 的定点修复，证据以自动化测试与门禁为主。
