# 前端应用入口注册表重构计划

## 背景

当前 `authorization_app` 已经承载 `appCode`、登录上下文、菜单和角色隔离等能力，但在产品语义上仍更像普通应用 CRUD。下一阶段需要把它升级为“逻辑应用 + 授权边界”，并用独立前端运行配置表达单体、混合和远程微前端形态。

目标不是马上全面微前端化，而是先把基础架构、代码边界和协议一步到位。部署形态后置决策：同一套代码未来既可以整体打包成 Mango Admin 单体，也可以按应用拆成多个远程前端入口。

## 设计原则

- 应用是入口和授权边界，不等同于后端 Maven 模块。
- 后端模块继续表达领域能力，前端应用表达用户入口和运行单元。
- 当前 `internal-admin` 继续作为默认内置管理端应用，确保现有登录、菜单、角色、租户能力不破坏。
- 先支持本地应用注册，再扩展远程微应用加载。
- 主框架只依赖统一协议，不直接依赖具体微前端引擎。
- `qiankun`、`wujie`、`iframe`、外链等作为 adapter 层实现，避免业务代码被引擎绑定。

## 管理后台双形态样例

`internal-admin` 是唯一默认管理后台逻辑应用。单体版和微前端版不是两个业务应用，而是同一应用的两种部署形态：

```text
单体版：internal-admin + LOCAL + EMBEDDED + apps/mango-admin
微前端版：internal-admin + MICRO_APP + HYBRID + apps/mango-admin-shell + apps/mango-admin-rbac-app
```

后端模块仍是能力模块，例如 `mango-authorization`、`mango-system`、`mango-workflow`；前端包仍是代码复用单元，例如 `@mango/rbac`；部署单元是实际运行产物，例如 `mango-admin` 或 `mango-admin-shell`。

## 目标模型

### 应用入口

把授权应用和前端运行配置组合成入口注册表视图：

```text
appCode              应用编码，稳定唯一
appName              应用名称
appType              LOCAL / MICRO_APP / IFRAME / EXTERNAL_LINK
deployMode           EMBEDDED / REMOTE / HYBRID
entryUrl             远程入口地址
mountPath            主框架挂载路径，如 /micro/workflow
activeRule           激活规则，如 /workflow/**
framework            vue3 / vue2 / react / iframe / link / other
version              当前版本
healthCheckUrl       健康检查地址
sandboxEnabled       是否启用沙箱
styleIsolation       NONE / SCOPED / SHADOW_DOM / IFRAME
icon                 图标
sort                 排序
status               状态
remark               备注
loginContexts        登录上下文
```

### 菜单关系

菜单继续归属 `appCode`，并补充页面运行类型：

```text
menu.appCode         归属应用
menu.pageType        LOCAL_ROUTE / MICRO_ROUTE / IFRAME / EXTERNAL_LINK / BUTTON
menu.component       本地组件路径，local 模式使用
menu.path            主框架路由路径
menu.redirect        重定向
menu.externalUrl     iframe 或外链地址
menu.meta            前端元信息
```

### 租户开通应用

新增租户应用开通关系，区分“平台有哪些应用”和“租户能用哪些应用”：

```text
tenant_app_binding
  id
  tenantId
  appCode
  status
  expireTime
  createTime
  updateTime
```

菜单包仍然用于控制租户在某个应用下可用的菜单范围。

## 后端任务

### P0：应用入口模型扩展

- 扩展 `authorization_app` 表字段：`app_type`、`deploy_mode`、`entry_url`、`mount_path`、`active_rule`、`framework`、`version`、`health_check_url`、`sandbox_enabled`、`style_isolation`。
- 更新 `AppCommand`、`AppVO`、`AuthorizationApp` entity。
- 保持旧数据兼容：`internal-admin` 默认 `appType=LOCAL`、`deployMode=EMBEDDED`。
- 应用管理 API 保持旧接口可用，同时返回新字段。

### P0：运行配置 API

新增面向前端 shell 的运行配置接口：

```text
GET /authorization/apps/runtime
GET /authorization/apps/runtime/{appCode}
```

返回当前登录用户、当前租户可访问的应用入口配置。需要按以下条件过滤：

- 应用状态启用。
- 当前租户已开通，平台租户可见全部或按规则可见。
- 当前登录上下文匹配 `realm + actorType`。
- 当前主体至少有该应用下菜单或角色授权。

### P0：租户应用开通

- 新增 `TenantAppBinding` 或等价模型。
- 新增应用开通、停用、查询接口。
- 机构/租户创建时默认开通 `internal-admin`。
- 菜单包绑定继续按 `appCode` 生效。

### P1：菜单页面类型

- 菜单表补充 `page_type`、`external_url`。
- 菜单管理支持本地路由、微应用路由、iframe、外链。
- `/authorization/menus/user` 返回前端需要的页面运行元信息。
- 角色授权树不展示孤儿按钮权限，不把脏权限节点作为一级菜单返回。

### P1：权限上下文校验

- 登录 token 继续携带 `appCode`。
- 远程应用加载时，前端传递 token、tenant、appCode，后端接口按 `appCode` 校验。
- Resource Access 继续使用 `principal.appCode()` 作为授权系统上下文。

### P2：健康检查与版本治理

- 支持应用健康检查 URL。
- 运行配置接口返回健康状态或上次检查结果。
- 预留版本、灰度、回滚字段，但第一阶段不做复杂发布中心。

## 前端任务

### P0：应用协议

新增统一协议：

```ts
export interface MangoFrontendApp {
  appCode: string;
  name: string;
  routes?: RouteRecordRaw[];
  menus?: MangoMenu[];
  permissions?: string[];
  mount?: (container: HTMLElement, props: MangoAppRuntime) => void | Promise<void>;
  unmount?: () => void | Promise<void>;
}

export interface MangoAppRuntime {
  token: string;
  tenantId?: string | number;
  appCode: string;
  userInfo: unknown;
  permissions: string[];
  theme: unknown;
  request: unknown;
  eventBus: unknown;
}
```

### P0：Adapter 分层

主框架只认 adapter：

```text
local adapter       本地包内置注册
iframe adapter      iframe 过渡
wujie adapter       可选远程微应用
qiankun adapter     可选远程微应用
link adapter        外链跳转
```

第一阶段实现 `local adapter` 和 `iframe/link adapter`，微前端引擎先预留接口。

### P0：单体模式保持可用

- `mango-admin-shell` 仍然可以把 `@mango/system`、`@mango/workflow`、`@mango/rbac` 等本地包一起打包。
- 当前菜单、标签页、布局、权限、主题不因应用注册表改造而失效。
- `internal-admin` 作为默认内置应用。

### P1：目录结构演进

目标结构：

```text
mango-ui/apps/mango-admin-shell
mango-ui/apps/system-app
mango-ui/apps/workflow-app
mango-ui/apps/devtools-app
mango-ui/packages/common
mango-ui/packages/auth-client
mango-ui/packages/theme
mango-ui/packages/app-runtime
```

第一阶段不强制迁移目录，可先新增 `packages/app-runtime`，把协议和 adapter 放进去。

### P1：应用管理页面

- 应用管理从普通 CRUD 升级为入口注册表维护页。
- 支持应用类型、部署模式、入口地址、挂载路径、激活规则、健康检查、沙箱、样式隔离配置。
- 表单要根据 `appType` 做字段显隐。

### P1：菜单管理页面

- 菜单页面类型选择：本地页面、微应用页面、iframe、外链、按钮。
- 本地页面显示组件路径。
- 微应用页面显示归属应用和挂载路径。
- iframe/外链显示 URL。

## 推荐执行顺序

1. 新增计划和 ADR，确认术语：应用、入口、菜单、模块、微应用。
2. 后端扩展 `authorization_app` 表和 VO/Command/Entity。
3. 后端新增 runtime config API。
4. 前端新增 `packages/app-runtime` 协议和 local/link/iframe adapter。
5. 前端应用管理页扩展字段。
6. 菜单管理页扩展页面类型。
7. 保持 `internal-admin` 本地单体运行。
8. 选一个低风险模块做远程加载 PoC，优先 `组件库` 或 `开发工具`。
9. 再评估 `workflow` 是否拆出为远程应用。

## 验证清单

- 登录后 token 仍包含 `appCode=internal-admin`。
- 首页、系统管理、协同办公菜单正常加载。
- 角色按应用授权，跨应用角色不串。
- 租户未开通应用时，runtime 配置不返回该应用。
- 应用停用后，菜单不可访问。
- LOCAL 应用单体打包可用。
- IFRAME/LINK 类型菜单可打开。
- MICRO_APP 类型字段可配置但第一阶段允许不启用引擎。
- 前端构建通过。
- 授权模块编译通过。
- 关键 E2E：登录、菜单导航、应用管理、菜单管理、角色授权、租户隔离。

## Worktree 建议

另一个窗口建议使用独立分支：

```bash
cd /Users/hardy/Work/mango
git fetch origin
git worktree add ../mango-frontend-app-registry-refactor -b feature/frontend-app-registry-refactor origin/feature/frontend-app-registry
cd ../mango-frontend-app-registry-refactor
```

避免两个工作区同时修改同一分支。
