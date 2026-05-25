# Mango UI Claude 入口

@../mango-pmo/rules/frontend/05-dev-flow.md
@../mango-pmo/rules/frontend/01-vue-code.md
@../mango-pmo/rules/frontend/06-monorepo-architecture.md
@../mango-pmo/rules/frontend/04-test.md

本文件是 `mango-ui` 的项目级协作入口。进入这个子项目后，先按 `../AGENTS.md` 判断是否需要执行 PMO preflight。

需要执行 preflight 时，推荐命令：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango-ui/**"
```

读取 preflight 输出中 `Must read` 的每一个文件原文后，再开始设计、编码或验证。简单问答、只读定位和快速查看不需要 preflight。

## 项目定位

`mango-ui` 是 Mango 的前端 Monorepo，当前由一个基座应用和多个业务包组成：

- `apps/mango-admin`：宿主应用
- `packages/common`：公共组件、hooks、utils、theme、公共 API
- `packages/api-schema`：跨包共享类型
- `packages/auth`：认证模块
- `packages/rbac`：权限模块
- `packages/system`：系统模块

## 必须先理解的边界

### 1. 依赖方向

允许：

```text
apps/mango-admin -> packages/*
packages/auth|rbac|system -> packages/common|api-schema
packages/common -> packages/api-schema
```

禁止：

- `packages/common` 依赖 `apps/mango-admin`
- 公共组件导入 `@/stores`、`@/api`、`@/config`
- 通过相对路径从包内跳回 `apps/`

### 2. 组件归属

- 简单基础 UI 直接用 `Element Plus`
- 复杂高复用业务组件收敛到 `packages/common/components`
- 已收敛到 `@mango/common` 的组件，不允许继续在基座保留第二份实现

### 3. 路由加载

- 菜单配置返回的组件路径必须通过 `componentsMap.ts` 或 `import.meta.glob()` 解析
- 禁止使用无法被 Vite 预分析的 `import(\`/src/${path}\`)`

### 4. Long ID 类型

- 后端 `Long`、雪花 ID、数据库主键、业务主键等标识字段在前端统一按字符串处理，公共类型使用 `ApiId`。
- API 类型、路由参数、表格行数据、上传文件标识和组件 model 中的 ID 禁止用 `Number(id)`、`parseInt(id)` 归一化。
- 只有页码、数量、金额、排序等真实数值字段可以转成 number。

## 开发流程

### 本地开发

```bash
cd mango-ui
pnpm install
pnpm dev
```

### 构建

```bash
cd mango-ui
pnpm build
```

### E2E

```bash
cd mango-ui/apps/mango-admin
npx playwright test
```

## 改动前检查

开始编码前先回答：

1. 这个改动属于基座、业务包还是公共包
2. 是否会改变公共导出、类型协议或 API 入口
3. 是否有形成反向依赖或循环依赖的风险
4. 是否会影响菜单加载、登录页、基础布局等主链路

## 交付标准

一个前端改动至少满足：

1. `pnpm dev` 能正常启动，浏览器控制台无红色模块解析错误
2. `pnpm build` 通过
3. `apps/mango-admin` 下 Playwright 通过相关用例
4. 文档、公共出口和类型声明同步更新

## 参考文档

- `../mango-pmo/rules/00-dev-flow.md`
- `../mango-pmo/rules/frontend/01-vue-code.md`
- `../mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `README.md`
