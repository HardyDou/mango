# Mango UI

`mango-ui` 是 Mango 开发底座的前端 Monorepo。它不是旧 `mango-web` 的简单搬运，而是把后台前端拆成“基座应用 + 业务包 + 公共包”的可演进结构。

当前工作区已经完成 `dev / build / E2E` 主链路修复，`mango-admin` 可以作为统一宿主装配认证、权限、系统管理和公共能力。

文档快速入口：

- [mango-ui 文档索引](./index.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [Sprint 13 迁移计划](../mango-docs/plans/2026-04-14-sprint-13-frontend-monorepo-migration.md)

## 项目结构

```text
mango-ui/
├── apps/
│   └── mango-admin/          # 前端基座，负责布局、路由聚合、权限拦截、应用级初始化
├── packages/
│   ├── api-schema/           # 跨包共享类型与接口协议
│   ├── auth/                 # 登录、认证相关视图与能力
│   ├── common/               # 公共组件、hooks、utils、theme、公共 API
│   ├── rbac/                 # 用户、角色、菜单、组织等权限模块
│   └── system/               # 系统管理模块
├── package.json
└── pnpm-workspace.yaml
```

## 架构原则

### 1. 基座负责装配，业务包负责交付

- `apps/mango-admin` 负责应用级入口、布局、路由注册、菜单挂载、权限守卫、主题初始化。
- `packages/*` 负责模块能力、页面、公共组件和公共 API。
- 应用级初始化脚本不要下沉到 `packages/common`，例如 `themeInit.ts`、权限注册指令、全局守卫。

### 2. 依赖方向必须单向

允许的依赖方向：

```text
apps/mango-admin -> packages/auth|rbac|system|common
packages/auth|rbac|system -> packages/common|api-schema
packages/common -> packages/api-schema
```

禁止：

- `packages/common` 反向依赖 `apps/mango-admin`
- 业务包直接依赖基座的 `@/stores`、`@/api`、`@/config`
- 公共组件通过相对路径回跳到 `apps/`

### 3. 公共组件只有一个归属

- 高复用、跨模块使用的业务组件统一放在 `packages/common/components`
- 统一从 `@mango/common` 导出，不允许一部分从 `@/components`，一部分从 `@mango/common`
- 基座中已下沉的组件不得保留第二份实现，避免双源漂移

当前已统一到 `@mango/common` 的典型组件包括：

- `Captcha`
- `Editor`
- `CodeEditor`
- `Upload`
- `ECharts`
- `Chat`
- `SSE`
- `Websocket`
- `ChinaArea`
- `OrgSelector`
- `FormCreate`
- `RightToolbar`
- `Sign`

### 4. 路由组件加载必须可被 Vite 静态分析

Vite 不支持完全变量化的绝对路径动态导入。菜单或后端返回的组件路径必须通过以下方式解析：

- 优先使用 `componentsMap.ts` 做显式注册
- 对基座遗留页面使用 `import.meta.glob('../views/**/*.vue')`
- 避免 `import(\`/src/${path}\`)` 这类无法被 Rollup 预分析的写法

## 开发流程

### 日常开发

```bash
pnpm install
pnpm dev
```

默认入口：

- 开发服务：`http://127.0.0.1:7777/#/login`

### 构建验证

```bash
pnpm build
```

### E2E 验证

```bash
cd apps/mango-admin
npx playwright test
```

建议最小验收顺序：

1. `pnpm dev` 可启动，登录页可渲染
2. 浏览器控制台无 `Failed to resolve import`、`404`、`ENOENT` 等模块错误
3. `pnpm build` 通过
4. `apps/mango-admin` 下 Playwright 通过

## 代码规范

### 组件与页面

- 基础 UI 优先直接使用 `Element Plus`
- 只有复杂业务组件才抽到 `packages/common/components`
- 页面不要直接依赖基座私有组件目录；优先从 `@mango/common` 获取复用能力
- 第三方样式如 `CodeMirror`、`WangEditor`、`Element Plus` 需要显式引入，不依赖隐式副作用

### Store 与状态

- Store 只用于应用级状态或明确的模块级状态
- 公共组件不要硬编码依赖基座 Store
- 如果组件需要外部状态，优先改成受控组件，通过 `props + emits` 暴露能力

### API 与类型

- 跨包共享类型优先放在 `packages/api-schema`
- `packages/common` 需要的公共请求封装放在包内 `api/`
- 不允许 `packages/common` 通过 `@/api/*` 访问基座 API

### 导入规则

- 跨包依赖优先使用包名导入，如 `@mango/common`
- 文件移动后必须回查相对路径，禁止留下失效的 `../storage`、`../api` 等引用
- 避免形成循环依赖，特别是 `main.ts -> init -> store -> common -> app`

## 交付与验收

一个前端改动完成前，至少要回答清楚下面四件事：

1. 组件归属是否清晰，是否只有一个实现来源
2. 是否引入了新的反向依赖或循环依赖
3. `pnpm dev`、`pnpm build`、`Playwright` 是否都验证过
4. 是否把新的约束同步到文档或公共出口

## 常见问题

### 登录页空白或 E2E 超时

优先检查：

- `pnpm dev` 控制台是否有模块解析错误
- 菜单组件路径是否能被 `componentsMap` 或 `import.meta.glob` 命中
- Demo 页面是否还在引用已经迁移的旧组件路径

### 新增公共组件时该放哪里

判断标准：

- 只在单个页面使用：留在模块内部
- 跨模块复用，且包含业务语义：放 `packages/common/components`
- 只是简单包装 `el-input`、`el-button`：通常不要封装

## 相关文档

- [前端代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [全局研发流程](../mango-pmo/rules/00-dev-flow.md)
