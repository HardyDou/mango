# 系统管理菜单规划

更新时间：2026-05-07

## 目标

系统管理菜单需要先覆盖后端已经具备的基础能力，再把用户、组织、应用、字典、行政区划等前端替换 mock 所需能力规划完整。菜单结构参考 PigX/UPMS 常见后台分层：用户、角色、菜单、部门/组织、岗位、字典、参数配置、日志审计等基础能力，但按 Mango 当前模块边界做收敛。

本规划采用“顶部系统 + 左侧菜单”模型：顶部菜单表示一个系统入口，左侧菜单表示该系统下的功能菜单。当前优先落地顶部系统“系统管理”，后续再扩展“开发平台”“业务平台”等顶部系统。

## PigX 菜单参考

已查看本地 PigX 数据库 `pigxx_boot.sys_menu`，其菜单模型可以作为信息架构参考：

| 层级 | PigX 示例 | 说明 |
|---|---|---|
| 顶部系统 | `parent_id = -1`：系统管理、业务平台、基础工具、协同办公、开发平台 | 顶部横向系统入口 |
| 系统内左侧菜单 | 系统管理下挂：权限管理、日志管理、字典管理、参数管理、国际化管理、终端管理、行政区划等 | 当前顶部系统下的左侧菜单 |
| 左侧分组 | 权限管理下挂：用户管理、菜单管理、角色管理、部门管理、岗位管理、租户管理 | 对高频权限基础能力做分组 |
| 按钮权限 | 菜单下挂：新增、修改、删除、导入导出、分配权限等 | 不进入左侧菜单，只作为权限点 |

PigX 的结构对 Mango 的启发：

1. 顶部系统不应该等同于普通左侧目录。Mango 可用 `parent_id = 0` 的根菜单表示顶部系统。
2. 左侧菜单应该只显示当前顶部系统下的功能，不把所有模块平铺在一个列表里。
3. 菜单树可以参考 PigX 的职责分组，但 Mango 权限编码、路由路径、接口语义必须使用自身规范，不照搬 PigX 的 `sys_*` 权限码和 `/admin/*` 路径。

## 当前后端能力审计

| 能力 | 后端接口 | 完成度 | 前端页面状态 | 菜单规划 |
|---|---|---|---|---|
| 用户管理 | `identity` 仅用户查询、`auth/info` 当前用户 | 不足 | `packages/rbac/src/views/user/index.vue` 当前偏静态 | 规划进菜单，但标记为待补后端 CRUD |
| 组织架构 | `/org/tree`、`/org/children`、`/org/detail` | 基础查询完成 | 有组织选择器，缺正式管理页 | 规划进菜单，先做树浏览/详情，再补 CRUD |
| 岗位管理 | `/post/page`、`/post/detail`、`POST/PUT/DELETE /post` | 完成 | 缺页面 | 规划进菜单，适合优先补页面 |
| 应用管理 | `/authorization/apps`、`/detail`、`POST/PUT/DELETE` | 完成 | 缺页面 | 规划进菜单 |
| 角色管理 | `/authorization/roles`、角色主体、角色菜单 | 完成 | `packages/rbac/src/views/role/index.vue` 已有但需联调 | 规划进菜单 |
| 菜单管理 | `/authorization/menus`、`/user`、`/detail`、`POST/PUT/DELETE` | 完成但写操作需 E2E 复测 | `packages/rbac/src/views/menu/index.vue` 已有 | 规划进菜单 |
| 接口资源 | `/authorization/api-resources/*` | 管理能力偏内部 | 缺页面 | 放入“安全与授权”，默认可后置 |
| 字典管理 | `/system/dict/type/*`、`/system/dict/data/*`、`/options` | 完成 | `packages/system/src/views/dict/index.vue` 已有，前端路径需对齐 | 规划进菜单 |
| 参数配置 | `/system/config/*` | 完成 | `packages/system/src/views/config/index.vue` 已有 | 规划进菜单 |
| 租户管理 | `/system/tenant/*` | 完成 | `packages/system/src/views/tenant/index.vue` 已有 | 规划进菜单 |
| 系统路由 | `/system/route/*` | 完成，排序契约需联调 | `packages/system/src/views/route/index.vue` 已有 | 规划进菜单，建议命名“路由管理” |
| 行政区划 | `/system/area/tree`、`/children`、`/detail`、`/adcode`、`POST/PUT/DELETE` | 完成，`active` 暂不处理 | 缺正式管理页，选择器已通过 E2E | 规划进菜单 |
| 国际化 | `/system/i18n/*` | 查询能力完成 | 缺管理页 | 规划进菜单，但后置 |
| 登录日志 | `/system/log/login/*` | 完成，前端详情路径需对齐 | `packages/system/src/views/login-log/index.vue` 已有 | 规划进菜单 |
| 操作日志 | `/system/log/operation/*` | 完成，导出暂缺 | `packages/system/src/views/operation-log/index.vue` 已有 | 规划进菜单 |
| 消息通知 | `/message/*` | 完成 | 缺页面 | 不放系统管理主组，规划为“消息中心” |
| 公共路径 | 前端 `/bff/permission/public-path`，后端缺 | 不足 | `packages/system/src/views/public-path/index.vue` 已有 | 暂不进正式菜单 |
| 上传/附件 | 前端 `/admin/upload/*`，后端缺 | 不足 | 上传演示页存在 | 暂不进正式菜单 |

## 推荐菜单结构

### 一级菜单

“系统管理”作为顶部系统入口，不再把所有条目平铺。左侧菜单建议按职责拆为：

1. 权限与组织
2. 平台应用
3. 基础配置
4. 运维审计

### 菜单树

| 层级 | 菜单名称 | 路由 | 组件 | 后端状态 | 优先级 |
|---|---|---|---|---|---|
| 顶部系统 | 系统管理 | `/system` | 顶部系统入口 | 已有 | P0 |
| 左侧目录 | 权限与组织 | `/system/permission` | 空目录 | 聚合目录 | P0 |
| 3 | 用户管理 | `/system/user` | `@/views/system/user/index.vue` | 待补完整用户 CRUD | P1 |
| 3 | 组织架构 | `/system/org` | 待新增 | 查询完成，CRUD 待补 | P1 |
| 3 | 岗位管理 | `/system/post` | 待新增 | 已完成 | P1 |
| 3 | 角色管理 | `/system/role` | `@/views/system/role/index.vue` | 已完成 | P0 |
| 3 | 菜单管理 | `/system/menu` | `@/views/system/menu/index.vue` | 已完成 | P0 |
| 3 | 接口资源 | `/system/api-resource` | 待新增 | 已完成偏内部 | P2 |
| 左侧目录 | 平台应用 | `/system/platform` | 空目录 | 聚合目录 | P1 |
| 3 | 应用管理 | `/system/app` | 待新增 | 已完成 | P1 |
| 3 | 租户管理 | `/system/tenant` | `@/views/system/tenant/index.vue` | 已完成 | P1 |
| 左侧目录 | 基础配置 | `/system/basic` | 空目录 | 聚合目录 | P0 |
| 3 | 参数配置 | `/system/config` | `@/views/system/config/index.vue` | 已完成 | P0 |
| 3 | 字典管理 | `/system/dict` | `@/views/system/dict/index.vue` | 已完成 | P0 |
| 3 | 行政区划 | `/system/area` | 待新增 | 已完成 | P1 |
| 3 | 路由管理 | `/system/route` | `@/views/system/route/index.vue` | 已完成 | P2 |
| 3 | 国际化 | `/system/i18n` | 待新增 | 查询完成 | P2 |
| 左侧目录 | 运维审计 | `/system/audit` | 空目录 | 聚合目录 | P1 |
| 3 | 登录日志 | `/system/login-log` | `@/views/system/login-log/index.vue` | 已完成 | P1 |
| 3 | 操作日志 | `/system/operation-log` | `@/views/system/operation-log/index.vue` | 已完成，导出暂缺 | P1 |

### 顶部系统规划

| 顶部系统 | 路由前缀 | 当前是否落地 | 说明 |
|---|---|---|---|
| 工作台 | `/dashboard` 或 `/home` | 已有首页能力，暂不纳入本轮系统菜单落库 | 登录后的默认入口，不承载管理菜单 |
| 系统管理 | `/system` | 本轮优先 | 当前基础能力主入口 |
| 开发平台 | `/dev` 或 `/developer` | 后置 | 代码生成、接口资源、模块资源、数据源等能力成熟后再启用 |
| 业务平台 | `/business` | 后置 | 业务模块接入后再规划 |
| 消息中心 | `/message` | 后置 | 消息通知可独立成顶部系统或顶栏入口，不建议塞进系统管理 |

## 不建议放入系统管理主菜单的能力

| 能力 | 建议位置 | 原因 |
|---|---|---|
| 当前用户信息、修改密码 | 顶栏用户菜单/个人中心 | 属于个人账号能力，不是系统配置能力 |
| 消息通知 | 一级“消息中心”或顶栏消息入口 | 面向业务用户，不应埋在系统管理下 |
| 验证码演示 | 开发/组件演示 | 不是管理对象 |
| 实时通信/SSE/WebSocket 演示 | 开发/组件演示 | 不是系统管理对象 |
| 公共路径 | 暂不展示 | 后端缺接口，展示会制造不可用菜单 |
| 附件/上传 | 暂不展示或后续“资源管理” | 后端上传能力未完成 |

## 首批落库菜单建议

首批只落后端已完成且前端有页面或马上要补页面的菜单，避免出现点击不可用。顶部系统使用“系统管理”，其子菜单作为左侧菜单渲染。

| 顺序 | 菜单 | 路由 | 说明 |
|---|---|---|---|
| 1 | 系统管理 | `/system` | 顶部系统 |
| 2 | 权限与组织 | `/system/permission` | 左侧目录 |
| 3 | 角色管理 | `/system/role` | 已有页面，后端完成 |
| 4 | 菜单管理 | `/system/menu` | 已有页面，后端完成 |
| 5 | 基础配置 | `/system/basic` | 左侧目录 |
| 6 | 参数配置 | `/system/config` | 已有页面，后端完成 |
| 7 | 字典管理 | `/system/dict` | 已有页面，需修前端契约 |
| 8 | 运维审计 | `/system/audit` | 左侧目录 |
| 9 | 登录日志 | `/system/login-log` | 已有页面，需修前端契约 |
| 10 | 操作日志 | `/system/operation-log` | 已有页面，需修前端契约 |
| 11 | 平台应用 | `/system/platform` | 左侧目录 |
| 12 | 租户管理 | `/system/tenant` | 已有页面，后端完成 |

## 第二批补齐菜单

| 顺序 | 菜单 | 需要完成 |
|---|---|---|
| 1 | 应用管理 | 新增前端页面，对接 `/authorization/apps` |
| 2 | 岗位管理 | 新增前端页面，对接 `/post/*` |
| 3 | 行政区划 | 新增正式管理页；只按层级/父级加载，不调用 `/system/area/active` |
| 4 | 组织架构 | 新增正式管理页；当前后端只有查询，CRUD 需确认是否补齐 |
| 5 | 用户管理 | 补后端用户 CRUD、角色分配、组织/岗位绑定后再联调 |
| 6 | 接口资源 | 新增页面前先明确是内部运维能力还是外部管理员能力 |
| 7 | 国际化 | 如要管理语言包，需补写接口；否则仅保留启动加载能力 |

## 推荐权限编码

统一沿用 `system:{resource}:{action}`，授权域内部资源也归入系统管理菜单时保持一致。

| 菜单 | 列表 | 新增 | 修改 | 删除 | 其他 |
|---|---|---|---|---|---|
| 用户管理 | `system:user:list` | `system:user:create` | `system:user:update` | `system:user:delete` | `system:user:reset-password`、`system:user:assign-role` |
| 组织架构 | `system:org:list` | `system:org:create` | `system:org:update` | `system:org:delete` |  |
| 岗位管理 | `system:post:list` | `system:post:create` | `system:post:update` | `system:post:delete` |  |
| 应用管理 | `system:app:list` | `system:app:create` | `system:app:update` | `system:app:delete` |  |
| 角色管理 | `system:role:list` | `system:role:create` | `system:role:update` | `system:role:delete` | `system:role:assign-menu`、`system:role:assign-subject` |
| 菜单管理 | `system:menu:list` | `system:menu:create` | `system:menu:update` | `system:menu:delete` |  |
| 字典管理 | `system:dict:list` | `system:dict:create` | `system:dict:update` | `system:dict:delete` |  |
| 参数配置 | `system:config:list` | `system:config:create` | `system:config:update` | `system:config:delete` | `system:config:update-value` |
| 行政区划 | `system:area:list` | `system:area:create` | `system:area:update` | `system:area:delete` |  |
| 登录日志 | `system:login-log:list` |  |  | `system:login-log:clean` |  |
| 操作日志 | `system:operation-log:list` |  |  | `system:operation-log:clean` | `system:operation-log:export` 后端暂缺 |

## 执行顺序

1. 更新授权菜单种子数据：先落首批菜单，管理员角色授权到这些菜单。`/system` 作为顶部系统，`/system/permission`、`/system/basic`、`/system/audit`、`/system/platform` 作为左侧目录。
2. 修前端顶部系统/左侧菜单渲染：顶部只展示根菜单，选择顶部系统后左侧只展示该系统的子菜单。
3. 修前端组件映射：补 `/system/app`、`/system/post`、`/system/area`、`/system/org` 等新增页面映射。
4. 按一个菜单一个 E2E 推进：
   - 参数配置
   - 字典管理
   - 登录日志
   - 操作日志
   - 租户管理
   - 角色管理
   - 菜单管理
5. 第二批新增页面：
   - 应用管理
   - 岗位管理
   - 行政区划管理
   - 组织架构管理
6. 用户管理最后处理，先补后端完整用户管理契约，再做页面联调。

## 结论

最佳方案是采用 PigX 的“顶部系统 + 左侧菜单”产品结构，但不照搬 PigX 的路径和权限码。在 Mango 里，“系统管理”先作为顶部系统入口，左侧使用“权限与组织 / 平台应用 / 基础配置 / 运维审计”四个目录承载功能。这样既兼容 PigX/UPMS 的基础模块范式，又能体现 Mango 当前“授权、组织、系统基础设施”分模块边界。
