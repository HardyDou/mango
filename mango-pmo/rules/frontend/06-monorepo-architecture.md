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

## 5. 组件开发规范

- **M* 组件优先**：所有业务包必须使用 `packages/common` 中导出的 `M*` 系列组件。
- **样式隔离**：必须使用 `CSS Modules` 或 `Scoped CSS`，禁止定义全局非变量样式。
- **变量共享**：必须引用 `common` 包中定义的 CSS 变量进行颜色和间距控制。
