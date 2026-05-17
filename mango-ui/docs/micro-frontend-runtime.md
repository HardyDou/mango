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

## 配置诊断

Shell 会对 `runtime-config.json` 做运行时校验：

- `profile` 只允许 `monolith / hybrid / micro`，非法值降级为 `monolith`。
- `mode` 只允许 `local / micro`，非法值降级为 `local`。
- `mode=micro` 必须配置 `entry`，否则该模块页面显示明确错误，不会 fallback 到其它子应用。
- `timeoutMs` 非法时降级为 `15000`。

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
- hybrid 下点击协同办公会加载 `c.mango.io:5182`。
- monolith 下不请求 `5181/5182`。
- 菜单数量、权限按钮、业务接口和单体入口一致。
