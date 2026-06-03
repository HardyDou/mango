# 前后端接口联调计划

更新时间：2026-05-07

## 执行原则

- 每次只处理一个页面或一个明确能力。
- 每完成一个能力，必须补充或执行对应 E2E 测试。
- E2E 通过后，才开始下一个能力。
- 不为了页面能跑而改乱接口语义；接口含义不清时先收敛契约。
- 暂不处理 `/system/area/active`，避免一次拉取过大数据。
- 前端真实联调必须使用 `VITE_USE_MOCK=false`。

## 环境

- 后端单体：`http://localhost:5555`
- 前端管理端：`http://localhost:7777`
- 前端工程：`/Users/hardy/Work/mango/mango-ui/apps/mango-admin`
- 后端工程：`/Users/hardy/Work/mango/mango`
- 登录账号：`admin / admin123`

## 阶段 1：基础登录与导航

| 顺序 | 页面/能力 | 前端位置 | 后端接口 | 状态 | E2E |
|---|---|---|---|---|---|
| 1 | 登录 | `packages/auth/src/views/login.vue` | `POST /auth/login` | 已通过 | 登录成功并进入首页 |
| 2 | 当前用户信息 | 登录后流程 | `GET /auth/info` | 已通过 | 真实登录后获取当前用户并写入页面会话 |
| 3 | 用户菜单导航 | `apps/mango-admin/src/config/menuLoader.ts` | `GET /authorization/menus/user?fmt=tree` | 已通过 | 侧边菜单使用后端菜单渲染 |

## 阶段 2：已具备后端能力的基础页面

| 顺序 | 页面/能力 | 前端位置 | 后端接口 | 状态 | E2E |
|---|---|---|---|---|---|
| 4 | 验证码组件 | `apps/mango-admin/src/views/demo/components/CaptchaView.vue` | `/captcha/types`、`/captcha/arithmetic`、`/captcha/block-puzzle`、`/captcha/verify` | 已通过 | 打开页面并生成验证码 |
| 5 | 行政区划选择器 | `packages/common/components/ChinaArea/index.vue` | `/system/area/tree`、`/system/area/children` | 已通过 | 选择省市区 |
| 6 | 组织选择器 | `apps/mango-admin/src/views/demo/components/OrgSelectorView.vue` | `/org/tree`、`/org/children` | 已通过 | 展开并选择组织 |
| 7 | 系统配置 | `packages/system/src/views/config/index.vue` | `/system/config/*` | 待执行 | 列表、详情、新增、编辑 |
| 8 | 租户管理 | `packages/system/src/views/tenant/index.vue` | `/system/tenant/*` | 待执行 | 列表、详情、新增、编辑、状态切换 |

## 阶段 3：接口存在但前端契约需修复

| 顺序 | 页面/能力 | 当前问题 | 目标接口 | 状态 | E2E |
|---|---|---|---|---|---|
| 9 | 字典管理 | 前端详情/删除路径使用 `/system/dict/type/{id}`，后端是 `/detail?id=` 和 `DELETE /type?id=` | `/system/dict/type/*`、`/system/dict/data/*` | 待执行 | 字典类型/数据 CRUD |
| 10 | 系统路由 | 路由管理维护能力已下线，动态路由由菜单管理承接 | `/system/route/*` | 不处理 | 不再保留路由管理 E2E |
| 11 | 登录日志 | 前端详情路径 `/system/log/login/{id}`，后端是 `/system/log/login/detail?id=` | `/system/log/login/*` | 待执行 | 列表、详情、统计 |
| 12 | 操作日志 | 导出接口前端存在，后端暂缺；详情路径需对齐 | `/system/log/operation/*` | 待执行 | 列表、详情、清理 |
| 13 | 菜单管理写操作 | 当前 `POST/PUT/DELETE /authorization/menus` 实测 401 | `/authorization/menus` | 待执行 | 菜单查询、新增、编辑、删除 |

## 阶段 4：需要新增页面或补接口

| 顺序 | 页面/能力 | 当前状态 | 目标 | 状态 | E2E |
|---|---|---|---|---|---|
| 14 | 行政区划管理 | 后端有 `detail/adcode/create/update/delete`，前端缺管理页 | 新增管理页；不接 `/active` | 待执行 | 树浏览、详情、自定义区划 CRUD |
| 15 | 消息中心 | 后端 `/message/*` 已有，前端缺页面 | 新增消息中心页面 | 待执行 | 列表、详情、已读、删除 |
| 16 | 修改密码 | 前端调用 `/user/password`，后端缺接口 | 补后端或调整前端入口 | 待执行 | 修改密码流程 |
| 17 | 上传组件 | 前端调用 `/admin/upload/*`，后端缺接口 | 补上传后端接口 | 待执行 | 文件/图片上传 |
| 18 | 公共路径管理 | 前端调用 `/bff/permission/public-path`，后端缺接口 | 明确归属后补接口或移除页面 | 待执行 | 公共路径 CRUD |
| 19 | 用户管理 | 页面静态数据，后端只有 identity 查询类接口 | 补用户管理接口或调整页面范围 | 待执行 | 用户列表/详情/角色分配 |
| 20 | 角色管理 | 页面静态数据，后端 `/authorization/roles*` 已有 | 接入角色 API | 待执行 | 角色 CRUD、菜单授权 |

## E2E 约定

- 测试目录：`/Users/hardy/Work/mango/mango-ui/apps/mango-admin/e2e`
- 每个能力一个独立 spec。
- 每个 spec 必须从真实登录开始，除非测试公开接口。
- 禁止使用 mock 数据完成联调验收。
- E2E 失败时停止后续联调，先修当前能力。
