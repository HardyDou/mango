# Mango UI

本项目是 Mango 开发底座的前端部分，采用 **Monorepo** 架构组织，支持 **微前端模块联邦**。

## 🏗️ 架构概览

参考后端的 SPI 与分层设计，本项目的包结构如下：

- **`apps/mango-admin`**: 基座应用。负责布局加载、路由聚合、权限拦截。
- **`packages/common`**: 对标 `mango-common`。包含 M* 原子组件、Hooks、Utils、Theme 变量。
- **`packages/api-schema`**: 对标 `mango-*-api`。定义跨模块调用的 TypeScript 接口协议。
- **`packages/auth`**: 认证模块。包含登录、SSO、Token 逻辑。
- **`packages/rbac`**: 权限模块。包含用户、角色、菜单管理页面。

## 🚀 核心特性

1. **开发/部署分离**：
   - 本地开发：启用 Mock 模式，不依赖后端服务。
   - 生产部署：切换为 Remote 模式，调用真实 API。
2. **原子化组件**：
   - 强制使用 `M*` 封装组件，实现 UI 风格的全局统一管控。
3. **依赖倒置 (DIP)**：
   - 视图层不依赖具体的 API 请求实现（Axios），而是依赖 `api-schema` 中的接口定义。

## 🛠️ 常用操作

```bash
# 安装依赖
pnpm install

# 启动主应用
pnpm dev

# 全量构建
pnpm build
```
