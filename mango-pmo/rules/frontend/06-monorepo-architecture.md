# 前端 Monorepo 架构规范 (Monorepo Architecture)

## 1. 核心原则

| 原则 | 说明 | 对标后端概念 |
|------|------|------------|
| **Monorepo** | 使用 pnpm workspaces 管理多包，逻辑强隔离 | Maven Modules |
| **API 抽象层** | 业务组件通过 Interface 调用 API，不关心实现 | API Module |
| **Provider 注入** | 开发态注入 Mock，生产态注入 Axios | Starter/Remote |
| **Host 基座化** | 主应用仅负责组装、布局与全局状态 | App Module |

## 2. 目录结构规范

```text
mango-ui/
├── apps/                  # 🚀 部署单元 (Applications)
│   └── mango-admin/       # 管理后台基座 (Host)
├── packages/              # 🧩 共享包与业务模块 (Packages)
│   ├── common/            # 核心公共库 (Utils, M* Components)
│   ├── api-schema/        # 全局 TS 接口定义 (Interfaces)
│   ├── auth/              # 认证业务模块
│   ├── rbac/              # 权限业务模块
│   └── system/            # 系统管理业务模块
├── package.json           # 根配置
└── pnpm-workspace.yaml    # 工作区定义
```

## 3. 依赖引用规则

1. **单向依赖**：`apps` 可以引用 `packages/*`；业务包可以引用 `common` 和 `api-schema`。
2. **禁止循环引用**：业务包之间（如 `rbac` 与 `system`）禁止直接相互引用。
3. **接口契约**：跨包的逻辑调用必须通过 `api-schema` 中定义的 TypeScript Interface。
4. **禁止反向依赖**：`packages/common`、`packages/auth`、`packages/rbac`、`packages/system` 禁止引用 `apps/mango-admin/src/**`。
5. **禁止跨层别名**：包内禁止使用宿主别名 `@/stores/*`、`@/api/*`、`@/config/*`、`@/i18n/*` 指向基座实现。

## 3.1 允许的依赖方向

```text
apps/mango-admin
  └── packages/common
  └── packages/auth
  └── packages/rbac
  └── packages/system

packages/auth
packages/rbac
packages/system
  └── packages/common
  └── packages/api-schema

packages/common
  └── 仅依赖第三方库 / 自身内部模块
```

禁止：
- `packages/common -> apps/mango-admin`
- `packages/rbac -> packages/system`
- `packages/system -> packages/rbac`
- 同一能力在 `apps` 与 `packages/common` 各维护一份实现

## 4. API 处理规范 (对标后端的 SPI)

每个业务包的 API 目录必须包含三部分：
- `types.ts`: 接口定义（对标 `api`）。
- `mock.ts`: 本地模拟实现（对标 `core`）。
- `provider.ts`: 真实 Axios 请求（对标 `remote`）。

在模块导出时，根据环境切换导出：
```typescript
// service/index.ts
import { MockProvider } from './mock';
import { RemoteProvider } from './provider';

export const useService = () => {
  return import.meta.env.VITE_USE_MOCK === 'true' ? new MockProvider() : new RemoteProvider();
}
```

### 4.1 公共组件 API 归属

- 被 `packages/common/components` 使用的 API 必须放到 `packages/common/api`。
- 禁止公共组件通过 `@/api/admin/*` 调宿主 API。
- 若某 API 仅某业务包使用，则保留在对应业务包内，不进入基座。

### 4.2 Mock 与真实接口一致性

- Mock 路径必须和真实接口路径保持一致，避免只在 `build` 阶段通过、`dev` 阶段因 MSW 路径不一致而失败。
- 更新 `msw` 版本或 handler 结构后，必须重新生成 `public/mockServiceWorker.js`。

## 5. 组件开发规范

- **直接使用 Element Plus**：对于基础组件，直接使用 `el-*`，不进行无意义的二次包装；仅针对复杂业务场景（如带 API 的选择器、高级表格）封装高阶组件。
- **样式隔离**：必须使用 `CSS Modules` 或 `Scoped CSS`，禁止定义全局非变量样式。
- **变量共享**：必须引用 `common` 包中定义的 CSS 变量进行颜色和间距控制。

## 6. 菜单与路由挂载规范

### 6.1 宿主职责

`apps/mango-admin` 只负责：
- 布局加载
- 路由聚合
- 权限拦截
- 全局初始化

不负责：
- 持有大量业务组件实现
- 持有跨业务域的重复 API 封装
- 作为公共组件唯一来源之外的第二实现

### 6.2 动态路由解析

- 后端返回的组件路径必须先标准化，再交由静态映射表或 `import.meta.glob` 解析。
- 跨包页面必须在映射表中显式注册，禁止依赖字符串拼接 `import()`。
- 遗留基座页面允许 `glob` 收集，但应逐步收敛为显式注册。

### 6.3 布局加载规则

- 同一布局模块不得同时静态导入和动态导入。
- 如布局切换频繁且组件很小，优先使用静态映射，避免 Rollup 产生重复 chunk warning。
- 若必须异步加载，不能再用同模块做 `loadingComponent` 的静态导入。

## 7. 迁移流程规范

### 7.1 从单体迁移到 Monorepo 的标准步骤

1. 识别公共能力，确定是否进入 `packages/common`。
2. 下沉代码时同步迁移：
   - 组件实现
   - `types.ts`
   - 测试
   - 样式
   - 依赖的 API
3. 在 `packages/common/index.ts` 或对应包入口完成导出。
4. 全局替换旧引用路径。
5. 删除原宿主中的重复实现。

### 7.2 迁移禁忌

- 只复制组件，不迁 API。
- 只改 `build`，不验证 `dev`。
- 保留两份实现，寄希望于“以后再删”。
- 允许 `common` 临时依赖宿主 store 或 i18n，再长期遗留。

## 8. 交付验收规范

### 8.1 必做检查

- `pnpm run build` 通过。
- `pnpm run dev` 可启动。
- 登录页可以渲染。
- 关键 E2E 可通过。
- 浏览器控制台无 `Failed to resolve import`、`ENOENT`、`404` 模块加载错误。

### 8.2 质量分级

| 等级 | 要求 |
|------|------|
| **阻塞** | 模块无法解析、登录页空白、动态路由挂载失败、E2E 登录失败 |
| **高优先** | 包层反向依赖、公共组件双份实现、运行时 chunk 循环 warning |
| **中优先** | 超大 chunk warning、低价值包装组件、未统一的类型导出 |
