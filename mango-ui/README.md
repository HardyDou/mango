# Mango UI

`mango-ui` 是 Mango 管理后台前端 Monorepo。当前同时支持单体入口和 Wujie 微前端入口：

- 单体入口 `apps/mango-admin`：默认交付形态，所有业务模块本地打包。
- 微前端 Shell `apps/mango-admin-shell`：主应用，负责登录态、菜单、权限、主题、TagsView、布局和运行配置。
- 子应用 `apps/mango-admin-rbac-app`、`apps/mango-admin-workflow-app`：业务页面运行单元，被 Shell 挂载时只渲染页面内容。

后端的逻辑应用、菜单、权限、租户和业务接口不因为前端部署形态变化而变化。单体、混合、微前端组合由前端静态运行配置控制。

## 目录结构

```text
mango-ui/
├── apps/
│   ├── mango-admin/              # 单体管理后台入口
│   ├── mango-admin-shell/        # 微前端主应用 Shell
│   ├── mango-admin-rbac-app/     # RBAC 子应用
│   └── mango-admin-workflow-app/ # Workflow 子应用
├── packages/
│   ├── admin-pages/              # 统一页面注册表，单体/Shell/子应用共用
│   ├── app-runtime/              # runtime-config、Wujie adapter、生命周期协议
│   ├── auth/                     # 登录、个人中心、修改密码等认证能力包
│   ├── common/                   # 公共组件、hooks、utils、theme、公共 API
│   ├── file/                     # 文件能力页面包
│   ├── rbac/                     # 用户、角色、菜单、组织、应用等权限能力页面包
│   ├── system/                   # 系统管理能力页面包
│   └── workflow/                 # 工作流能力页面包
├── docs/
│   └── micro-frontend-runtime.md # 微前端运行说明
├── deploy/nginx/                 # 生产部署 nginx 示例
├── scripts/                      # 本地微前端启动/预览脚本
└── pnpm-workspace.yaml
```

## 核心模型

- 逻辑应用：`internal-admin`，表示管理后台授权边界。
- 能力模块：`mango-authorization`、`mango-system`、`mango-workflow`、`mango-file` 等，贡献菜单和业务页面能力。
- 主应用 Shell：读取当前用户在 `internal-admin` 下的菜单树，统一管理导航和页面分发。
- 子应用：只负责业务页面渲染，不拥有主导航、登录页、主题设置、TagsView。
- 页面运行策略：由前端 `runtime-config.json` 决定模块本地渲染还是远程加载。

菜单和权限仍来自后端。不要为了切换单体/微前端去改数据库菜单、权限或后端接口。

## 规范入口

长期前端规则只维护在 `mango-pmo/rules/frontend/**`。本 README 只说明 `mango-ui` 使用入口和模块定位。

- [Vue 代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [Element Plus UI 规范](../mango-pmo/rules/frontend/02-element-plus-ui.md)
- [组件开发规范](../mango-pmo/rules/frontend/03-component-development.md)
- [前端测试规范](../mango-pmo/rules/frontend/04-test.md)
- [前端开发流程](../mango-pmo/rules/frontend/05-dev-flow.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)

模块能力入口见 [Mango 能力地图](../mango-docs/capabilities/README.md#6-前端与-cli-能力) 和各 `packages/*/README.md`。

## 选择哪种形态

默认优先使用单体入口。微前端入口用于验证模块独立开发、独立部署和按配置组合交付。

| 场景 | 推荐入口 | 说明 |
| --- | --- | --- |
| 日常业务页面开发 | `mango-admin` 单体 | 启动最少，和默认交付一致 |
| 调整 Shell、主题、菜单、TagsView | `mango-admin-shell` | 需要验证主应用框架行为 |
| 开发 RBAC 独立运行单元 | `mango-admin-rbac-app` + Shell | 子应用独立调试，最终由 Shell 挂载 |
| 开发 Workflow 独立运行单元 | `mango-admin-workflow-app` + Shell | 子应用独立调试，最终由 Shell 挂载 |
| 验证混合部署 | Shell + runtime-config | 不改后端和数据库，只改前端静态配置 |

## 单体方式使用

单体方式是默认交付形态：`apps/mango-admin` 自己承载登录、布局、菜单、主题、TagsView 和全部业务页面。

安装依赖：

```bash
pnpm install
```

启动：

```bash
pnpm -C mango-ui --filter mango-admin dev -- --host 0.0.0.0 --port 5175
```

访问：

```text
http://127.0.0.1:5175
```

如果要代理到指定后端：

```bash
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:5555 \
pnpm -C mango-ui --filter mango-admin dev -- --host 0.0.0.0 --port 5175
```

构建：

```bash
pnpm -C mango-ui --filter mango-admin build
```

使用说明：

- 单体入口可以直接使用 `@mango/rbac`、`@mango/system`、`@mango/workflow`、`@mango/file` 等业务包。
- 业务页面放置和页面注册规则见 [组件开发规范](../mango-pmo/rules/frontend/03-component-development.md) 和 [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)。
- 页面注册表用法见 [@mango/admin-pages](./packages/admin-pages/README.md)。

## 微前端方式使用

微前端方式由 Shell 统一承载后台框架，RBAC、Workflow 等模块可按配置改为远程子应用。

启动全部微前端开发服务：

```bash
pnpm -C mango-ui dev:micro
```

默认服务：

```text
Shell:    http://127.0.0.1:5176
RBAC:     http://127.0.0.1:5181
Workflow: http://127.0.0.1:5182
```

如果要指定后端：

```bash
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:5555 pnpm -C mango-ui dev:micro
```

如果要让同事通过局域网访问，dev server 监听 `0.0.0.0`。当前微前端脚本和子应用 Vite 配置已按 `0.0.0.0` 监听，可用你的局域网 IP 访问：

```text
http://192.168.x.x:5176
```

跨域域名联调建议配置 hosts：

```text
127.0.0.1 a.mango.io
127.0.0.1 b.mango.io
127.0.0.1 c.mango.io
```

然后访问：

```text
Shell:    http://a.mango.io:5176
RBAC:     http://b.mango.io:5181
Workflow: http://c.mango.io:5182
```

微前端构建：

```bash
pnpm -C mango-ui build:micro
```

微前端 E2E：

```bash
pnpm -C mango-ui dev:micro
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

使用说明：

- Shell 是微前端形态的正式访问入口；子应用端口用于研发调试。
- Shell 承载菜单、权限、主题、TagsView 和登录态；子应用渲染当前业务页面。
- 业务接口通过 Shell `/api` 代理进入后端。

## 运行配置

微前端 Shell 优先读取 `apps/mango-admin-shell/public/runtime-config.json`，其次读取环境变量，默认是 `monolith`。

示例：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-authorization": {
      "mode": "micro",
      "runtimeCode": "mango-admin-rbac-app",
      "entry": "http://b.mango.io:5181/"
    },
    "mango-system": {
      "mode": "local",
      "runtimeCode": "mango-admin-system-local"
    },
    "mango-workflow": {
      "mode": "micro",
      "runtimeCode": "mango-admin-workflow-app",
      "entry": "http://c.mango.io:5182/"
    }
  }
}
```

配置含义：

- `profile=monolith`：全部本地渲染，不请求子应用。
- `profile=hybrid`：部分模块本地，部分模块远程。
- `profile=micro`：已拆模块远程，未拆模块可以继续本地。
- 远程入口白名单和 HTTPS 要求见 [前端开发流程](../mango-pmo/rules/frontend/05-dev-flow.md)。
- 切换部署形态只替换前端静态配置，不改后端、不改数据库。

详细说明见 [微前端运行说明](./docs/micro-frontend-runtime.md)。

## 典型使用流程

### 本地开发单体业务页面

1. 在对应业务包开发页面，例如 `packages/rbac` 或 `packages/workflow`。
2. 在 `packages/admin-pages/src/defaults.ts` 注册页面。
3. 启动单体：

```bash
pnpm -C mango-ui --filter mango-admin dev -- --host 0.0.0.0 --port 5175
```

4. 登录 `http://127.0.0.1:5175` 验证菜单和页面。
5. 构建验证：

```bash
pnpm -C mango-ui --filter mango-admin build
```

### 本地开发微前端业务页面

1. 在业务包开发页面。
2. 在 `packages/admin-pages` 注册页面。
3. 启动微前端：

```bash
pnpm -C mango-ui dev:micro
```

4. 独立打开子应用端口做页面调试：

```text
RBAC:     http://127.0.0.1:5181
Workflow: http://127.0.0.1:5182
```

5. 打开 Shell 做真实挂载验证：

```text
http://127.0.0.1:5176
```

6. 确认点击菜单时由 Shell 挂载子应用，子应用不显示主导航。

### 切换单体/混合/微前端组合

只改 `apps/mango-admin-shell/public/runtime-config.json` 或部署态同名静态文件。

全部本地渲染：

```json
{
  "profile": "monolith",
  "modules": {
    "mango-authorization": { "mode": "local" },
    "mango-system": { "mode": "local" },
    "mango-workflow": { "mode": "local" }
  }
}
```

RBAC/Workflow 远程，System 本地：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-authorization": {
      "mode": "micro",
      "runtimeCode": "mango-admin-rbac-app",
      "entry": "http://127.0.0.1:5181/"
    },
    "mango-system": {
      "mode": "local"
    },
    "mango-workflow": {
      "mode": "micro",
      "runtimeCode": "mango-admin-workflow-app",
      "entry": "http://127.0.0.1:5182/"
    }
  }
}
```

切换后刷新 Shell 即可验证，不需要改后端、不需要改数据库、不需要改菜单。

## 生产部署方式

### 单体生产部署

构建：

```bash
pnpm -C mango-ui --filter mango-admin build
```

部署产物：

```text
apps/mango-admin/dist
```

nginx 只需要代理单体静态资源和后端 `/api`：

```text
https://admin.example.com      -> apps/mango-admin/dist
https://admin.example.com/api  -> mango-backend
```

### 微前端生产部署

构建：

```bash
VITE_MANGO_ALLOWED_REMOTE_ORIGINS=https://rbac.example.com,https://workflow.example.com \
pnpm -C mango-ui build:micro
```

部署产物：

```text
admin.example.com    -> apps/mango-admin-shell/dist
rbac.example.com     -> apps/mango-admin-rbac-app/dist
workflow.example.com -> apps/mango-admin-workflow-app/dist
```

Shell 代理业务接口：

```text
https://admin.example.com/api -> mango-backend
```

Shell 发布 `runtime-config.json`：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-authorization": {
      "mode": "micro",
      "runtimeCode": "mango-admin-rbac-app",
      "entry": "https://rbac.example.com/"
    },
    "mango-system": {
      "mode": "local"
    },
    "mango-workflow": {
      "mode": "micro",
      "runtimeCode": "mango-admin-workflow-app",
      "entry": "https://workflow.example.com/"
    }
  }
}
```

生产部署检查点见 [前端开发流程](../mango-pmo/rules/frontend/05-dev-flow.md) 和 [微前端运行说明](./docs/micro-frontend-runtime.md)。

## 开发中心菜单

`开发中心` 是前端开发态菜单，只在 `import.meta.env.DEV` 下追加：

```text
开发中心
└── 组件库
    ├── 富文本编辑器
    ├── 代码编辑器
    ├── 文件上传
    ├── 数据图表
    ├── 功能指令
    └── 示例页面
```

使用说明：

- 开发中心菜单来自前端开发态追加，不写入后端菜单表。
- 开发中心承载组件库、示例页和调试页。
- 页面注册入口见 [@mango/admin-pages](./packages/admin-pages/README.md)。

## 页面注册入口

所有业务页面都通过 `packages/admin-pages` 统一注册：

```text
moduleCode + componentPath -> page loader
```

使用入口：

- 页面注册表说明见 [@mango/admin-pages](./packages/admin-pages/README.md)。
- 页面放置、注册和复用规则见 [组件开发规范](../mango-pmo/rules/frontend/03-component-development.md)。
- Monorepo 依赖边界见 [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)。

## 主应用与子应用职责

Shell 负责：

- 登录态和退出跳转
- 用户菜单树和权限上下文
- 顶部、左侧、TagsView、主题
- runtime-config 加载与诊断
- Wujie 子应用挂载、卸载、错误边界
- `/api` 业务接口代理入口

子应用负责：

- 当前业务页面渲染
- 使用 Shell 注入的 token、tenantId、userInfo、permissions、theme、request
- 暴露统一 `mount(container, runtime)` 和 `unmount()` 协议

## 包边界入口

依赖方向：

```text
apps/* -> packages/*
packages/rbac|system|workflow|file|auth -> packages/common|api-schema|app-runtime
packages/common -> packages/api-schema
```

包边界、组件归属、共享类型、样式和临时代码处理见：

- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [组件开发规范](../mango-pmo/rules/frontend/03-component-development.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)

## 构建和验证

单体构建：

```bash
pnpm -C mango-ui --filter mango-admin build
```

微前端构建：

```bash
pnpm -C mango-ui build:micro
```

Shell 构建：

```bash
pnpm -C mango-ui --filter mango-admin-shell build
```

微前端 E2E：

```bash
pnpm -C mango-ui dev:micro
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

页面、菜单、Shell 和子应用验收口径见 [前端测试规范](../mango-pmo/rules/frontend/04-test.md)。

## 提交前检查

改动范围对应的检查入口：

```bash
pnpm -C mango-ui --filter mango-admin build
pnpm -C mango-ui --filter mango-admin-shell build
pnpm -C mango-ui build:micro
```

如果改了微前端运行逻辑，补充：

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

提交前规则见 [前端开发流程](../mango-pmo/rules/frontend/05-dev-flow.md)。

## 相关文档

- [微前端运行说明](./docs/micro-frontend-runtime.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [全局研发流程](../mango-pmo/rules/00-dev-flow.md)
