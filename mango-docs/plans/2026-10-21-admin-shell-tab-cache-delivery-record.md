# 标准交付记录

## Admin Shell 内容滚动重置与页签缓存

## 1. 元数据

- 任务 ID：issue-1083-scroll-and-tab-cache
- 关联 Issue：[HardyDou/mango#1083](https://github.com/HardyDou/mango/issues/1083)
- 交付模式：STANDARD
- 需求影响：L2 - 影响所有管理端页面路由切换后的滚动位置和页签缓存行为
- 方案风险：L2 - 涉及 Shell 主内容区滚动宿主、Vue KeepAlive 宿主和授权菜单默认值迁移
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issue-1083`，用户在当前非 main 工作区明确要求补齐页签缓存
- 评审选择：AGENT_ONLY；用户未要求外部同行评审

## 2. 目标与范围

- 目标：路由切换后主内容区回到顶部；切换页签保留列表页搜索条件。
- 成功条件：
  - `route.path` 变化后 `.layout-main-scroll` 的 `scrollTop` 为 0。
  - 滚动重置链路缺少宿主时输出明确错误，不静默跳过。
  - 成员管理等页面菜单的搜索条件在页签切换、左侧菜单往返、顶部系统分组切换后仍保留。
  - 管理端可以在菜单管理中关闭单个菜单的路由缓存。
- 处理范围：Admin Shell 滚动 composable、`LayoutMain` 暴露方法、`MangoAdminParentView` KeepAlive 宿主、RBAC 菜单表单、Authorization 菜单默认值与迁移。
- 不处理范围：业务页面自身的数据刷新策略、微前端 Wujie 实例缓存、发布版本号与 CHANGELOG。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | 管理端任意页面 | 页面滚动后切换菜单或浏览器返回 | 主内容区 `scrollTop` 归零 | 停留在旧位置 | 浏览器脚本断言切换后 `scrollTop=0` |
| R-02 | 管理端布局 | 滚动宿主未挂载 | 控制台输出 `[mango-shell] failed to reset content scroll: ...` 并返回 `false` | 静默无操作 | 单元测试断言错误日志与返回值 |
| R-03 | 管理端页签 | 页面菜单开启路由缓存，输入搜索条件后切换页签或切换顶部系统分组再返回 | 搜索条件保留 | 搜索条件被清空 | 浏览器脚本断言输入框值保留 |
| R-04 | RBAC 菜单管理 | 编辑页面菜单 | 提供「路由缓存」开关并可保存 | 无配置入口 | 浏览器脚本断言开关存在、可切换、保存后回读一致 |
| R-05 | Authorization | 新库初始化或既有库升级 | 页面菜单 `keep_alive` 默认 1，目录和按钮为 0 | 默认不缓存 | 迁移后按 `menu_type` 分组统计 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01/R-02 | `LayoutMain` 通过 `defineExpose` 提供 `updateScrollbar`、`resetContentScroll`，`useScrollbar` 只调用公开方法并在宿主缺失时 `console.error` 并返回 `false` | `packages/admin-shell/src/layout/component/main.vue`、`composables/useScrollbar.ts`、`layout/index.vue`、`layout/main/classic.vue`、`layout/main/transverse.vue` | 回退为直接访问 `layoutMainScrollbarRef` |
| D-02 | R-03 | `MangoAdminParentView` 的 `<keep-alive>` 常驻挂载，按 `route.meta.keepAlive` 决定是否渲染到缓存子树，避免切到未缓存页面时整个缓存宿主被卸载 | `packages/admin-shell/src/layout/routerView/parent.vue` | 恢复 `v-if` 挂载 `keep-alive` 的旧结构 |
| D-03 | R-05 | 复用既有 `authorization_menu.keep_alive`，把列默认值改为 1 并为既有页面菜单回填 1；模块菜单同步与手工创建在未显式传值时按 `menuType=2` 默认 1 | `V3__enable_keep_alive_for_page_menus.sql`、`MenuService.java`、`AppModuleService.java` | 执行 `UPDATE authorization_menu SET keep_alive=0` 并回退代码 |
| D-04 | R-04 | 复用 RBAC 菜单表单，新增「路由缓存」开关绑定 `keepAlive`，按钮类型统一提交 0 | `packages/rbac/src/views/menu/index.vue` | 移除表单项，后端继续使用既有字段 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 | 1 | `packages/admin-shell/src/layout/component/main.vue`、`composables/useScrollbar.ts` | 重置链路走公开方法，缺失时明确失败 |
| I-02 | D-01 | 2 | `layout/index.vue`、`layout/main/classic.vue`、`layout/main/transverse.vue` | 三个布局使用类型化宿主 ref |
| I-03 | D-02 | 3 | `layout/routerView/parent.vue` | KeepAlive 宿主常驻，切换未缓存页面不清空缓存 |
| I-04 | D-05 | 4 | `V3__enable_keep_alive_for_page_menus.sql`、`MenuService.java`、`AppModuleService.java` | 页面菜单默认缓存，目录和按钮不缓存 |
| I-05 | D-04 | 5 | `packages/rbac/src/views/menu/index.vue`、`packages/rbac/README.md`、`packages/admin-shell/README.md` | 菜单管理可配置路由缓存并有说明 |
| I-06 | 全部 | 6 | `packages/admin-shell/src/__tests__/scrollbar.spec.ts`、`layoutStability.spec.ts` | 单元测试覆盖重置失败与宿主常驻结构 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-02 | 前端单元测试 | `cd mango-ui && pnpm -F @mango/admin-shell test` | PASS，19 files，96 tests | Vitest 输出 |
| R-03 | 前端单元测试 | 同上，`keeps the tab cache host mounted when a non-cached page is active` | PASS | Vitest 输出 |
| R-01/R-03/R-04 | 真实浏览器（Chrome，本地工作区 102） | `.runtime/verify-issue-1083-scroll-reset-strong.mjs`；`.runtime/verify-search-retained.mjs`；`.runtime/verify-menu-switch3.mjs` | R-01 PASS（303 → 0 → 0）；R-03 PASS（`RETAIN_1` 在页签切换和顶部系统分组切换后保留）；R-04 PASS（开关显示为已开启，可切换并保存成功）；console 错误 0 | 脚本输出与截图 `.runtime/issue-1083-strong-*.png`、`.runtime/menu-cache-switch.png` |
| R-05 | 数据库回读 | 启动后端后按 `menu_type, keep_alive` 分组统计 | PASS：目录 33 条 `keep_alive=0`，页面菜单 95 条 `keep_alive=1`，按钮 5 条 `keep_alive=0` | MySQL 查询输出 |
| 全部改动 | Java 质量门禁 | `mvn -o -Drevision=1.0.0-mango-102 -pl :mango-authorization-core -am verify` | PASS，BUILD SUCCESS，`mango-authorization-core` 61 tests，0 failures | Maven 输出 |
| 全部改动 | 前端 PR 质量门禁 | `cd mango-ui && pnpm check:pr -- --base=origin/main --head=HEAD` | PASS，affected frontend check PASS | 命令输出 |
| 全部改动 | 文档门禁 | `audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`、`check-capability-docs.mjs --base origin/main --head HEAD` | PASS | 命令输出 |
| 全部改动 | Diff 检查 | `git diff --check` | PASS | 命令输出 |

## 7. 例外与剩余风险

- 未覆盖微前端 Wujie 实例缓存路径；本次只验证单体管理端 `router-view` 模式，`runtime-outlet` 模式沿用既有 `localPageCache` 行为。
- 缓存默认开启后，页面返回时可能展示上次数据；需要实时数据的页面应在 `activated` 自行刷新，本次不改业务页面。
- 菜单管理的 ESLint 仍有存量 `vue/multi-word-component-names` 基线告警，与本次字段改动无关。
