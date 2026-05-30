# Mango Admin Runtime 产品化 Sprint 3 设计说明

## 1. 目标

Sprint 3 交付 capability manifest 依赖解析能力，让业务项目在 `@mango/admin` custom preset 下只声明自己需要的 Mango 能力，运行时自动补齐可解析的 Mango 基础依赖，并对缺失、冲突和循环依赖给出明确失败报告。

本阶段必须证明：

- full preset 继续复用完整 Mango Admin 能力，不出现回归。
- custom preset 能从内置能力目录自动补齐 `auth`、`rbac` 等必需依赖。
- custom preset 对缺失能力、冲突能力、循环依赖失败关闭，错误信息可定位。
- 解析结果包含 dependency report，后续 CLI 和 runtime 可复用同一套解析逻辑。
- E2E 必须保留 full/custom 截图、布局报告和截图识别结论。

## 2. 范围

### 2.1 本阶段范围

- 扩展 `MangoCapabilityManifest`，增加 `conflicts` 兼容字段。
- 在 `@mango/admin-pages/core` 提供共享 dependency resolver。
- `@mango/admin` full/standard/custom preset 使用 resolver 产出 resolution report。
- custom preset 从默认 capability catalog 自动补齐必需依赖。
- `package:check` 增加 dependency graph 契约检查。
- 增加 custom preset E2E，验证只声明业务所需 capability 时仍集成真实 Mango Admin Shell 和真实内置能力。

### 2.2 非本阶段范围

- 不引入远程 capability registry。
- 不删除旧混合包。
- 不改变后端菜单、权限、登录接口契约。
- 不做业务模块生成器的新模板升级。

## 3. 设计决策

### 3.1 manifest 2.0 兼容扩展

`requires` 和 `optional` 保持字符串数组，避免破坏已有包。新增可选 `conflicts?: string[]`，用于声明不能同时启用的 capability。

默认依赖解析规则：

- 显式传入 capability 为 selected。
- 默认 Mango catalog 为可自动补齐来源。
- `requires` 是强依赖，custom preset 可从 catalog 自动补齐。
- `optional` 不自动安装，只进入报告。
- `conflicts` 命中已选能力时失败。
- 循环依赖失败，错误报告包含循环路径。

### 3.2 共享解析器

解析器放在 `@mango/admin-pages/core`：

- 输入：selected capabilities、catalog capabilities、preset、autoInstallRequired。
- 输出：ordered capabilities、diagnostics、report。
- 调用方：`@mango/admin` runtime 直接使用；后续 CLI 可通过 `@mango/admin-pages/core` 复用。

### 3.3 preset 行为

- `full`：默认完整 Mango catalog，仍保持全部内置 capability。
- `standard`：默认基础管理能力集合。
- `minimal`：只注册显式能力，但仍产出报告，不自动补齐。
- `custom`：自动补齐强依赖，失败关闭。

## 4. 测试策略

### 4.1 新特性测试

- `pnpm -F @mango/admin-pages test` 覆盖自动补齐、缺失、冲突、循环依赖和顺序。
- `pnpm -F @mango/admin test` 覆盖 preset resolution report。
- `pnpm package:check` 覆盖 manifest 2.0 字段和依赖图契约。
- `pnpm admin:custom-preset-e2e` 验证 custom preset 真实启动、登录、菜单、布局、颜色、截图和解析报告。

### 4.2 回归测试

- `pnpm package:check`
- `pnpm package:build`
- `pnpm -F @mango/admin-pages test`
- `pnpm -F @mango/admin test`
- `pnpm admin:full-preset-e2e`

## 5. 完成标准

- dependency resolver 由 `@mango/admin-pages/core` 导出。
- `@mango/admin` custom preset 可自动补齐内置强依赖。
- 缺失、冲突、循环依赖均失败关闭且报告明确。
- full/custom 两组 E2E 均通过，证据包含截图和 layout-report。
- Sprint 3 交付台账全部 DONE，delivery contract check 通过。
