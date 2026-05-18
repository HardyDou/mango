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

## 必须遵守的架构规则

下面这些规则不是建议。违反后会直接破坏“单体可用、微前端可组合、代码可复用、部署可切换”的架构特性。

### 1. 部署形态只能由前端配置决定

必须：

- 通过 `runtime-config.json` 或构建环境变量切换 `monolith / hybrid / micro`。
- 保持后端菜单、权限、租户、业务接口不因部署形态变化而变化。

禁止：

- 为了切换微前端去改数据库菜单。
- 为了某个子应用创建新的后端逻辑应用。
- 在后端接口里写死某个模块必须本地或远程加载。

破坏后果：

- 同一套客户数据无法在单体和微前端之间切换。
- 交付时需要改库才能组合模块，失去灵活部署价值。

### 2. Shell 是唯一后台框架所有者

必须：

- Shell 统一管理登录态、菜单树、权限上下文、主题、顶部、左侧、TagsView。
- 子应用被 Shell 挂载时只渲染当前业务页面。

禁止：

- 子应用重复渲染主导航、顶部栏、TagsView、全局主题设置。
- 子应用自己请求全局菜单并决定菜单结构。
- Shell 里写某个业务模块的页面细节。

破坏后果：

- Shell 和子应用 UI 变成两套系统。
- 菜单、权限、主题、标签页状态不一致。
- 子应用无法被任意组合进同一个后台。

### 3. 业务页面只能有一个源码来源

必须：

- RBAC 页面放 `packages/rbac`。
- Workflow 页面放 `packages/workflow`。
- System 页面放 `packages/system`。
- File 页面放 `packages/file`。
- 认证和个人能力页面放 `packages/auth`。
- 单体、Shell 本地模式、子应用独立调试都复用这些业务包。

禁止：

- 为了子应用复制一份 RBAC 或 Workflow 页面源码。
- Shell 和子应用各维护一套页面组件。
- 把业务页面新增到 `apps/mango-admin-shell`。

破坏后果：

- 同事在原业务包里的改动无法自动进入微前端。
- 单体和微前端页面行为分叉，后续合并成本失控。

### 4. 页面加载必须经过统一页面注册表

必须：

- 新增菜单页面后，在 `packages/admin-pages/src/defaults.ts` 注册。
- 保证后端菜单 `component` 能命中页面注册表。
- 单体、Shell、本地渲染和子应用调试共用同一份映射。

禁止：

- 在 Shell 内临时写一份 `component -> import()` 映射。
- 在子应用内再写一份不一致的页面映射。
- 使用 Vite 无法静态分析的动态导入，例如完全变量化的 `import(path)`。

破坏后果：

- 本地能跑，生产构建缺 chunk 或白屏。
- 同一个菜单在单体、Shell、子应用里打开不同页面。

### 5. 菜单归属按菜单树判断，不按 URL 前缀硬编码

必须：

- 用菜单树父子关系判断当前页面属于哪个一级菜单。
- 允许父级菜单路径和子页面路径不是同一个前缀。

禁止：

- 用 `route.path.startsWith(topMenu.path)` 作为唯一归属判断。
- 为了适配前端菜单强行修改后端菜单路径。

破坏后果：

- `开发中心 > 组件库 > /components/*` 这类菜单会跳错顶部或回首页。
- 旧页面路径无法平滑迁移到新菜单结构下。

### 6. 开发中心只能存在于开发环境

必须：

- `开发中心` 只在 `import.meta.env.DEV` 下追加。
- 组件库、示例页面、调试页面只能作为开发态辅助入口。

禁止：

- 把开发中心写入后端菜单表。
- 让开发中心进入测试/生产菜单。
- 把真实业务模块挂到开发中心。

破坏后果：

- 客户交付菜单被开发调试入口污染。
- 权限验收和生产菜单结构不一致。

### 7. 子应用接口必须走 Shell 注入上下文

必须：

- 子应用使用 Shell 传入的 `request`、token、tenantId、userInfo、permissions。
- 业务接口统一走 Shell `/api` 代理。

禁止：

- 子应用自己维护一套登录态。
- 子应用正式场景直连后端并自行处理跨域、401、租户上下文。

破坏后果：

- token 过期、租户切换、权限按钮表现不一致。
- 每个子应用都要维护一套后端跨域策略。

### 8. 生产远程入口必须精确白名单

必须：

- 生产使用 `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` 精确声明远程来源。
- 生产远程入口使用 HTTPS。
- 子应用 CORS 只允许 Shell origin。

禁止：

- 生产使用 `Access-Control-Allow-Origin: *` 承载后台访问。
- 生产使用 host 宽泛白名单绕过 exact origin。
- 在数据库里硬编码远程入口。

破坏后果：

- 后台资源加载边界不清晰，存在安全风险。
- 同一套构建无法在不同环境通过静态配置替换完成部署。

### 9. 提交不能混入无关自动格式化

必须：

- 运行 `eslint --fix` 后检查 `git diff`。
- 只提交和任务相关的文件。

禁止：

- 把历史文件的大面积格式化混进功能提交。
- 为了通过局部任务去重构同事正在开发的业务页面。

破坏后果：

- 与其他同事分支冲突显著增加。
- 微前端结构改造和业务功能改动混在一起，无法评审和回滚。

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

适用规范：

- 单体入口可以直接使用 `@mango/rbac`、`@mango/system`、`@mango/workflow`、`@mango/file` 等业务包。
- 新增业务页面仍要放在对应业务包，不要放回 `apps/mango-admin` 私有目录。
- 单体和微前端必须复用 `packages/admin-pages` 的页面注册规则。

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

如果要让同事通过局域网访问，dev server 必须监听 `0.0.0.0`。当前微前端脚本和子应用 Vite 配置已按 `0.0.0.0` 监听，可用你的局域网 IP 访问：

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

适用规范：

- 用户正式访问 Shell，不直接访问子应用端口。
- 子应用独立端口只用于研发调试。
- Shell 管菜单、权限、主题、TagsView 和登录态。
- 子应用只渲染当前业务页面。
- 业务接口通过 Shell `/api` 代理统一进入后端。

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

约束：

- `profile=monolith`：全部本地渲染，不请求子应用。
- `profile=hybrid`：部分模块本地，部分模块远程。
- `profile=micro`：已拆模块远程，未拆模块可以继续本地。
- 生产环境只允许 `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` 精确白名单，不允许 host 宽泛白名单。
- 生产环境远程入口必须使用 HTTPS。
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

生产要求：

- `runtime-config.json` 建议 `Cache-Control: no-store`。
- 子应用静态资源 CORS 只允许 Shell origin。
- 不使用 `Access-Control-Allow-Origin: *` 承载后台生产访问。
- 业务接口由 Shell 域名统一代理，子应用不直接暴露后端代理。
- 切换组合方式只替换 Shell 的 `runtime-config.json`。

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

规范：

- 不写入后端菜单表。
- 不进入测试/生产构建菜单。
- 只承载组件库、示例页、调试页。
- 新增开发页必须注册到 `packages/admin-pages`，确保单体和 Shell 使用同一份页面映射。
- 开发中心可以保留历史页面路径，例如 `/components/editor`，但菜单归属必须按菜单树关系判断，不允许用 URL 前缀硬编码。

## 页面注册规范

所有业务页面都通过 `packages/admin-pages` 统一注册：

```text
moduleCode + componentPath -> page loader
```

要求：

- 单体、本地 Shell、子应用独立运行必须复用同一份页面映射。
- 不复制业务页面源码。
- 不在 Shell 和子应用各维护一套不一致映射。
- 新增菜单组件路径后，必须同步检查 `packages/admin-pages/src/defaults.ts`。
- 后端菜单返回的 `component` 要能被页面注册表命中。

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

子应用禁止：

- 重复实现主导航、登录页、TagsView。
- 直接决定全局菜单结构。
- 绕过 Shell 自行维护后端跨域和登录跳转。
- 复制 `@mango/rbac`、`@mango/workflow` 等业务页面源码。

## 代码规范

依赖方向：

```text
apps/* -> packages/*
packages/rbac|system|workflow|file|auth -> packages/common|api-schema|app-runtime
packages/common -> packages/api-schema
```

禁止：

- `packages/common` 反向依赖 `apps/*`。
- 业务包直接依赖基座私有路径，如 `@/stores`、`@/api`、`@/config`。
- 公共组件通过相对路径回跳到应用目录。
- 为了微前端把同一业务页面复制到多个目录。
- 用临时 import 后缀、私有中转文件、重复映射表等方式绕过共享能力归属问题。
- 把只解决当前报错、没有根因说明和验证结论的代码作为最终实现。

组件和 API：

- 跨模块复用组件放 `packages/common/components`。
- 认证相关页面放 `packages/auth`。
- RBAC 业务页面放 `packages/rbac`。
- Workflow 业务页面放 `packages/workflow`。
- 跨包共享类型优先放 `packages/api-schema`。
- ID 类型按字符串处理，除页码、数量、金额、排序等真实数值外，不要对 ID 做 `Number(id)` 或 `parseInt(id)`。
- 单体和微前端共用的菜单树、图标、权限、运行时工具放 `packages/common` 或 `packages/app-runtime`，业务代码直接依赖共享包稳定入口。

临时代码规则：

- 临时代码只能用于本地排查，提交前必须删除或升级为正式方案。
- 正式方案需要符合依赖方向和模块归属，不能靠局部特殊写法掩盖架构问题。
- 修复运行态问题时，必须覆盖开发态访问和生产构建验证。

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

最小验收：

1. `5175` 单体登录后菜单、主题、TagsView、业务页面正常。
2. `5176` Shell 登录后菜单和单体一致。
3. `hybrid` 下点击 RBAC 菜单加载 RBAC 子应用。
4. `hybrid` 下点击 Workflow 菜单加载 Workflow 子应用。
5. `monolith` 下不请求 `5181/5182`。
6. 菜单数量、按钮权限、业务接口和单体一致。
7. 浏览器控制台无业务 401/404/500 和白屏错误。

## 提交前检查

至少执行和改动范围匹配的检查：

```bash
pnpm -C mango-ui --filter mango-admin build
pnpm -C mango-ui --filter mango-admin-shell build
pnpm -C mango-ui build:micro
```

如果改了微前端运行逻辑，补充：

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

如果运行 `lint:eslint --fix`，必须检查是否改到了无关历史文件。无关自动格式化不要混入提交。

## 相关文档

- [微前端运行说明](./docs/micro-frontend-runtime.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [全局研发流程](../mango-pmo/rules/00-dev-flow.md)
