# Mango 组件与能力使用指南 ForAI

## 1. 定位

本文给人和 AI 使用 Mango 前端能力时快速判断依赖、入口、样式、注册和验收方式。

长期规则仍以 `mango-pmo` 为唯一来源。本文只说明如何使用当前已整理出的 Mango npm 能力包。

## 2. 选择入口

| 场景 | 推荐入口 | 说明 |
| --- | --- | --- |
| 生成全量管理端 | `mango init --preset full` | 适合框架验收、内置能力全量演示、平台后台；CLI npm 包为 `@mango/cli` |
| 生成按需业务后台 | `mango init --preset custom --modules ...` | 适合业务项目，只安装选择的可选能力；CLI npm 包为 `@mango/cli` |
| 给已有生成项目追加能力 | `mango add <module>` | 只更新 CLI 管理的集成文件 |
| 手工接入核心后台 | `@mango/admin` + `@mango/admin/style.css` | 需要自己配置 feature 和 registrar |
| 手工接入全量后台 | `@mango/admin/full` + `@mango/admin/style-full.css` | 全量能力聚合入口 |

## 3. 必选与可选能力

`authorization` 和 `system` 是核心能力，业务后台默认保留，不作为可取消模块。

可选模块：

| CLI 模块 | feature | npm 包 | registrar | 样式 |
| --- | --- | --- | --- | --- |
| `workflow` | `workflow` | `@mango/workflow` | `registerMangoWorkflowAdminPages` | `@mango/workflow/style.css` |
| `workflow-example` | 依赖 `workflow` | `@mango/workflow-business-example` | `registerMangoWorkflowBusinessExampleAdminPages` | `@mango/workflow-business-example/style.css` |
| `file` | `file` | `@mango/file` | `registerMangoFileAdminPages` | `@mango/file/style.css` |
| `template` | `template` | `@mango/template` | `registerMangoTemplateAdminPages` | `@mango/template/style.css` |
| `notice` | `notice` | `@mango/notice` | `registerMangoNoticeAdminPages`, `registerMangoNoticeAdminShell` | `@mango/notice/style.css` |
| `numgen` | `numgen` | `@mango/numgen` | `registerMangoNumgenAdminPages` | `@mango/numgen/style.css` |
| `calendar` | `calendar` | `@mango/calendar` | `registerMangoCalendarAdminPages` | `@mango/calendar/style.css` |

## 4. CLI 用法

安装 CLI：

先配置 npm registry。全局安装 CLI 时使用用户级 `~/.npmrc`；项目内安装依赖时使用企业项目根目录 `.npmrc`：

```ini
registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
@mango:registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

再安装：

```bash
npm install -g @mango/cli@1.0.16
```

包名必须使用 `@mango/cli`。不要安装未 scoped 的 `mango-cli` npm 包；`mango-cli` 只是安装后的兼容命令名之一，推荐日常使用 `mango`。

全量项目：

```bash
mango init demo-admin --preset full --topology monolith
```

按需项目：

```bash
mango init claim-admin --preset custom --modules workflow,template,file --topology monolith
```

只保留核心系统能力：

```bash
mango init core-admin --preset custom --modules none --topology monolith
```

追加能力：

```bash
mango add notice --project-dir ./claim-admin
```

`mango add` 只改 CLI 管理的集成文件：

- `mango.config.json`
- `frontend/package.json`
- `frontend/src/main.ts`
- `frontend/public/runtime-config*.json`
- `backend/pom.xml`

业务自有页面、接口、README、业务文档不由 `mango add` 覆盖。

## 5. 手工接入 custom 后台

`frontend/src/main.ts` 示例：

```ts
import { createMangoAdminApp } from '@mango/admin';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';
import '@mango/admin/style.css';
import '@mango/workflow/style.css';
import '@mango/template/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  features: ['workflow', 'template'],
  featureRegistrars: [
    registerMangoWorkflowAdminPages,
    registerMangoTemplateAdminPages,
  ],
  devCenter: {
    deployEnv: import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE,
  },
}).mount();
```

要点：

- `features` 决定菜单过滤和能力可见性。
- `featureRegistrars` 决定页面 registry 是否注册。
- 样式按能力包随包引入，避免复制到业务项目里维护第二份。
- `workflow-example` 只用于业务示例表单；普通审批能力只选 `workflow`。

## 6. 手工接入 full 后台

```ts
import { createMangoAdminApp } from '@mango/admin/full';
import '@mango/admin/style-full.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  features: 'full',
}).mount();
```

`style-full.css` 聚合 Element Plus、Mango theme、shell、核心包和所有可选能力样式。业务项目不需要再逐个引入可选包样式。

## 7. 部署模式配置

运行时配置文件位于生成项目前端：

```text
frontend/public/runtime-config.json
```

本地单体模式：

```json
{
  "profile": "monolith",
  "modules": {
    "mango-authorization": { "mode": "local", "runtimeCode": "mango-admin-rbac-local" },
    "mango-system": { "mode": "local", "runtimeCode": "mango-admin-system-local" },
    "mango-workflow": { "mode": "local", "runtimeCode": "mango-admin-workflow-local" },
    "mango-template": { "mode": "local", "runtimeCode": "mango-admin-template-local" }
  }
}
```

微前端模式：

```json
{
  "profile": "hybrid",
  "modules": {
    "mango-authorization": {
      "mode": "micro",
      "runtimeCode": "mango-admin-rbac-app",
      "entry": "http://b.mango.io:5181/"
    },
    "mango-system": { "mode": "local", "runtimeCode": "mango-admin-system-local" },
    "mango-workflow": {
      "mode": "micro",
      "runtimeCode": "mango-admin-workflow-app",
      "entry": "http://c.mango.io:5182/"
    },
    "mango-template": {
      "mode": "micro",
      "runtimeCode": "mango-admin-template-app",
      "entry": "http://d.mango.io:5183/"
    }
  }
}
```

## 8. 验收口径

每次接入或调整能力包后建议检查：

- 页面能登录并拿到真实菜单。
- 左侧菜单、顶栏、标签页、主内容同时存在。
- 主内容有业务字段、按钮、表格或空状态，不是空白页。
- 浏览器 console、页面异常、失败接口要记录。
- 微前端页面要同时检查 shell 文档和 wujie 子应用内容。
- 样式验收要截图，不能只看测试断言。

推荐命令：

```bash
npm run build
npm run test:e2e
```

Mango 仓内 Sprint 8 模式矩阵验收脚本：

```bash
node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-8/verify-mode-matrix.mjs
```

## 9. AI 使用提示

AI 接到业务项目接入 Mango 能力的任务时，按顺序确认：

1. 项目是 full 还是 custom。
2. 需要哪些 CLI 模块，不需要哪些模块。
3. `package.json` 是否有对应 npm 包。
4. `main.ts` 是否有 feature、registrar 和样式引入。
5. 后端 `pom.xml` 是否有对应 starter。
6. `runtime-config.json` 是 local 还是 micro。
7. 验收截图是否覆盖真实页面内容。

不要只因为菜单出现就声明能力可用；应打开代表性页面并检查主内容。
