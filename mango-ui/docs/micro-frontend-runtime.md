# Mango 管理后台微前端运行说明

## 模型

`internal-admin` 是逻辑应用和授权边界。菜单、权限、租户、后端接口不因为部署形态变化而变化。

前端部署形态只由 `mango-admin-shell/public/runtime-config.json` 决定：

- `monolith`：所有模块本地渲染，不请求子应用。
- `hybrid`：部分模块本地渲染，部分模块通过 Wujie 加载远程子应用。
- `micro`：已拆出的模块远程加载，未拆出的模块仍可保持本地渲染。

## 运行单元

```text
主应用：apps/mango-admin-shell
RBAC 子应用：apps/mango-admin-rbac-app
Workflow 子应用：apps/mango-admin-workflow-app
共享页面注册表：packages/admin-pages
运行协议与 Wujie adapter：packages/app-runtime
```

Shell 负责登录态、菜单树、权限上下文、主题、TagsView、错误态和 `/api` 代理。子应用只负责当前业务页面渲染。

## 职责边界

主应用 Shell 是唯一的后台框架所有者：

- 统一加载 `/authorization/menus/user?fmt=tree&appCode=internal-admin`。
- 统一渲染顶部、左侧、TagsView、用户菜单、主题设置。
- 统一维护 token、tenantId、userInfo、permissions、request、eventBus、theme。
- 统一根据 `runtime-config.json` 决定本地渲染还是 Wujie 挂载。
- 统一处理远程加载失败、超时、卸载、运行日志。

子应用是业务页面运行单元：

- 被 Shell 加载时只渲染当前菜单对应的业务页面。
- 独立访问只用于开发调试，可以有轻量 debug shell，但不能作为正式主框架。
- 不读取全局后台菜单，不渲染主导航，不维护 TagsView。
- 不直接决定业务模块是否可见，模块可见性由后端菜单/权限和 Shell 统一控制。

不要把“逻辑应用”“能力模块”“前端运行单元”混成一个概念：

```text
internal-admin      # 逻辑应用，授权边界
mango-authorization # 能力模块，贡献 RBAC 菜单和页面
mango-admin-rbac-app # 前端运行单元，承载 RBAC 页面远程运行
```

## 本地开发

先配置 hosts：

```text
127.0.0.1 a.mango.io
127.0.0.1 b.mango.io
127.0.0.1 c.mango.io
```

启动三端：

```bash
pnpm -C mango-ui dev:micro
```

访问：

```text
Shell: http://a.mango.io:5176
RBAC: http://b.mango.io:5181
Workflow: http://c.mango.io:5182
```

后端默认通过 Shell 的 `/api` 代理到 `http://127.0.0.1:18081`。

## 配置切换

`runtime-config.json` 示例：

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
      "entry": "http://c.mango.io:5182/",
      "preload": true,
      "alive": false
    }
  }
}
```

切换形态只替换这个前端静态配置文件，不改后端、不改数据库、不改菜单、不改权限。

## 页面分发规范

Shell 点击菜单后的分发顺序：

1. 读取菜单 `moduleCode` 和 `component`。
2. 读取当前前端 runtime config。
3. `mode=local`：从 `packages/admin-pages` 查找页面并本地渲染。
4. `mode=micro`：按 `entry` 使用 Wujie 加载子应用，并把当前菜单和 runtime 上下文传入。
5. `IFRAME / EXTERNAL_LINK`：按菜单运行类型处理。

页面注册表是唯一映射来源：

```text
packages/admin-pages/src/defaults.ts
```

新增或调整业务页面时必须遵守：

- 后端菜单的 `component` 要能命中页面注册表。
- 单体、Shell 本地模式、子应用独立调试应复用同一套页面组件。
- 不在 Shell 和子应用各写一套页面映射。
- 不复制 `@mango/rbac`、`@mango/workflow` 等业务页面源码。
- 菜单归属按菜单树关系判断，不按 URL 前缀硬编码。父菜单路径和子页面路径可以不同前缀。

## 开发中心菜单

`开发中心` 是前端开发态辅助菜单，只在 Vite dev 环境追加，不来自后端，也不进入测试/生产菜单。

用途：

- 组件库
- 示例页面
- 本地调试页

约束：

- 不写入数据库菜单表。
- 不参与客户交付菜单。
- 不承载真实业务模块入口。
- 新增开发页也要走 `packages/admin-pages` 注册，避免 Shell 和单体页面来源不一致。
- 如果历史页面路径仍是 `/components/*` 或 `/demo/*`，可以保留直接访问路径，但菜单树上归属 `开发中心 > 组件库`。

## 配置诊断

Shell 会对 `runtime-config.json` 做运行时校验：

- `profile` 只允许 `monolith / hybrid / micro`，非法值降级为 `monolith`。
- `mode` 只允许 `local / micro`，非法值降级为 `local`。
- `mode=micro` 必须配置 `entry`，否则该模块页面显示明确错误，不会 fallback 到其它子应用。
- `timeoutMs` 非法时降级为 `15000`。
- 生产构建默认禁止 `http://` 远程入口。
- 生产构建的绝对远程入口必须命中 `VITE_MANGO_ALLOWED_REMOTE_ORIGINS`，生产不启用 host 宽泛白名单。
- 生产构建加载不到 `runtime-config.json` 时会失败关闭，不再静默回退到默认配置。

开发态可在浏览器控制台查看：

```js
window.__MANGO_RUNTIME_CONFIG_DIAGNOSTICS__
window.__MANGO_RUNTIME_DEBUG__
window.__MANGO_MICRO_APP_EVENTS__
```

这些诊断只用于排障，不参与业务权限判断。

## 跨域要求

Wujie 加载子应用 HTML、JS、CSS 时，子应用域名必须允许 Shell origin。

开发态 Vite 已限制允许来源：

```text
http://localhost:5176
http://127.0.0.1:5176
http://a.mango.io:5176
```

生产态不要使用 `Access-Control-Allow-Origin: *` 配合凭证。推荐按环境白名单返回明确 origin，例如：

```text
Access-Control-Allow-Origin: https://admin.example.com
Access-Control-Allow-Credentials: true
Vary: Origin
```

业务接口由 Shell 统一代理。子应用在 Wujie 环境会从主应用注入的 `apiBaseUrl` 调用 `https://admin.example.com/api`，避免每个子应用重复暴露后端跨域策略。

## 生产部署

推荐按运行单元独立部署静态产物：

```text
admin.mango.io    -> apps/mango-admin-shell/dist
rbac.mango.io     -> apps/mango-admin-rbac-app/dist
workflow.mango.io -> apps/mango-admin-workflow-app/dist
```

Shell 域名统一代理业务接口：

```text
https://admin.mango.io/api -> http://mango-backend:18081
```

子应用不直接暴露后端代理给浏览器正式入口；被 Shell 加载时使用主应用注入的 `apiBaseUrl`。这样业务接口、token 过期、租户上下文和 401 跳转都由 Shell 统一处理。

nginx 示例见：

```text
mango-ui/deploy/nginx/mango-admin-micro.conf
```

`runtime-config.json` 必须按环境作为可替换静态配置发布，建议禁用缓存：

```nginx
location = /runtime-config.json {
    add_header Cache-Control "no-store";
    try_files $uri =404;
}
```

切换部署形态时只替换 `admin.mango.io/runtime-config.json`：

```bash
# 单体组合，所有模块本地渲染
cp runtime-config.monolith.json runtime-config.json

# 混合组合，RBAC/Workflow 远程，System 本地
cp runtime-config.hybrid.json runtime-config.json
```

生产子应用 CORS 使用明确白名单：

```nginx
map $http_origin $mango_micro_allowed_origin {
    default "";
    "https://admin.mango.io" $http_origin;
}
```

不要用 `Access-Control-Allow-Origin: *` 承载带凭证的生产后台。

## 子应用加载策略

远程模块支持两个生产参数：

```json
{
  "preload": true,
  "alive": false
}
```

- `preload=true`：Shell 加载运行配置后提前拉取子应用资源，适合常用模块。
- `alive=true`：Wujie 保活子应用实例，页面状态不丢；默认关闭，避免后台管理页面跨菜单残留筛选条件、弹窗和事件监听。

构建产物默认按域名根路径部署。若运维要挂到子路径，构建时设置 `VITE_PUBLIC_PATH`：

```bash
VITE_PUBLIC_PATH=/rbac/ pnpm -C mango-ui --filter mango-admin-rbac-app build
VITE_PUBLIC_PATH=/workflow/ pnpm -C mango-ui --filter mango-admin-workflow-app build
```

对应 `runtime-config.json` 的 `entry` 要指向同一个子路径。

生产 Shell 构建时必须声明允许加载的子应用来源：

```bash
VITE_MANGO_ALLOWED_REMOTE_ORIGINS=https://rbac.mango.io,https://workflow.mango.io pnpm -C mango-ui --filter mango-admin-shell build
```

`VITE_MANGO_ALLOWED_REMOTE_HOSTS` 只允许用于开发、测试或本地 preview 便利配置；`prd/prod/production` 环境会忽略它。生产必须使用 exact origin，避免同 host 不同 scheme/port 或宽泛 host 配置误放行。

本地用 `vite preview` 做生产产物验收时，因为仍是 HTTP 域名，需要显式打开本地开关：

```bash
VITE_MANGO_E2E=true \
VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES=true \
VITE_MANGO_ALLOWED_REMOTE_ORIGINS=http://b.mango.io:4181,http://c.mango.io:4182 \
pnpm -C mango-ui build:micro

pnpm -C mango-ui preview:micro
PLAYWRIGHT_BASE_URL=http://a.mango.io:4176 PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

`VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES=true` 只用于本地或预发 HTTP 验收。正式生产必须使用 HTTPS 远程入口，并用 `VITE_MANGO_ALLOWED_REMOTE_ORIGINS` 做精确来源白名单。

## 研发规范

新增能力模块页面：

1. 页面放到对应业务包，例如 `packages/rbac`、`packages/system`、`packages/workflow`。
2. 在 `packages/admin-pages` 注册 `moduleCode + componentPath`。
3. 后端菜单只维护业务菜单和权限，不维护部署形态。
4. 需要远程运行时，为该模块新增或复用子应用入口。
5. 通过 `runtime-config.json` 切换 local/micro。

修改 Shell：

- 只能处理框架级能力：布局、导航、主题、TagsView、runtime、错误边界。
- 不直接写业务页面逻辑。
- 不把某个业务模块的页面细节写死在 Shell。

修改子应用：

- 保持 `mount(container, runtime)` / `unmount()` 协议稳定。
- 被 Shell 挂载时不得显示顶部、左侧、TagsView。
- 业务接口走 Shell 注入的请求上下文。
- 独立访问只做开发调试，不作为客户正式入口。

提交前检查：

```bash
pnpm -C mango-ui --filter mango-admin build
pnpm -C mango-ui --filter mango-admin-shell build
pnpm -C mango-ui build:micro
```

改了 Wujie、runtime config、主题同步、子应用生命周期时，补充：

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui test:micro --project=chromium
```

运行 `eslint --fix` 后必须检查工作区，避免把无关历史文件自动格式化混入提交。

## 常见误区

- 不要把每个能力模块都建成后端逻辑应用。默认是 `internal-admin` 集成多个能力模块。
- 不要通过改数据库菜单切换单体/微前端。部署形态由前端 runtime config 控制。
- 不要让子应用拥有主菜单。菜单由 Shell 统一管理。
- 不要把业务接口拆成每个子应用一套跨域策略。正式业务接口由 Shell 统一代理。
- 不要用 `Access-Control-Allow-Origin: *` 承载生产后台带凭证访问。
- 不要用 URL 前缀判断菜单归属，应按菜单树父子关系判断。

## 主题同步和运行日志

Shell 会把当前主题快照注入 Wujie 子应用，并在主题、布局、组件尺寸变化时通过 `theme-change` 事件同步。子应用通过 `@mango/app-runtime/vue-micro` 的 `bindMangoRuntimeTheme` 应用 CSS 变量，保持按钮、表格、表单主色和暗色状态一致。

运行期日志默认保留最近 200 条到：

```js
window.__MANGO_RUNTIME_LOGS__
```

生产错误会输出到 `console.error`，可用 `setMangoRuntimeLogger()` 对接 Sentry、日志网关或自建埋点服务。建议至少上报：

- `runtime-config-error`
- `micro-app-error`
- `micro-app-timeout`
- `micro-app-mount`
- `micro-app-unmount`

## 验证

构建：

```bash
pnpm -C mango-ui build:micro
```

E2E：

```bash
pnpm -C mango-ui dev:micro
cd mango-ui/apps/mango-admin-shell
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm test:e2e
```

验收点：

- hybrid 下点击系统管理会加载 `b.mango.io:5181`。
- hybrid 下点击审批中心会加载 `c.mango.io:5182`。
- monolith 下不请求 `5181/5182`。
- 菜单数量、权限按钮、业务接口和单体入口一致。
