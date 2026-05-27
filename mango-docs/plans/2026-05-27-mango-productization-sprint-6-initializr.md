# Mango 产品化 Issue #26 Sprint 6 Mango Initializr

## 1. 背景

Issue #26 要求提供类似 Spring Initializr / Vue init 的新项目启动体验。Sprint 5 已提供 `mango-business-starter`，本 Sprint 在前端工具包中提供可本地执行的初始化 CLI。

## 2. 目标

提供 `create-mango-app` 包，支持 `npm create mango-app@latest <project>` 和 `mango init <project>` 两种入口，基于 `mango-business-starter` 生成业务项目骨架。

## 3. 范围

- 新增 `mango-ui/packages/create-mango-app`。
- 提供 `create-mango-app` 和 `mango` bin。
- 支持项目名、模块名、包名、聚合名、部署模式和版本号参数。
- 复制模板并替换文件路径和文件内容占位符。
- 生成完成后输出后续验证命令。
- 提供 CLI 自测脚本，生成临时项目并校验关键文件。

## 4. 不做什么

- 不提供在线 Initializr 服务。
- 不做交互式 TUI。
- 不安装依赖、不启动生成项目。
- 不生成真实业务逻辑。
- 不替代后续 `mango add module/page/api/contract`。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/create-mango-app`：新增初始化 CLI 包。
- `mango-business-starter`：作为模板输入源。
- `mango-docs/plans`：新增 Sprint 6 文档和台账。

### 5.2 接口变化

新增 CLI 命令：

```bash
npm create mango-app@latest <project>
mango init <project>
```

支持参数：

```text
--module <module>
--aggregate <aggregate>
--package <basePackage>
--group-id <groupId>
--version <projectVersion>
--topology <monolith|microservice>
--template <templatePath>
--force
```

### 5.3 数据变化

无数据库变更。

### 5.4 菜单/页面/权限变化

生成项目继承 Sprint 5 模板中的资源清单和 page registry，CLI 不直接修改 Mango 菜单数据。

### 5.5 测试范围

- CLI 帮助输出。
- CLI 生成临时项目。
- 校验占位符被替换。
- 校验关键后端、前端、拓扑和业务 PMO 文件存在。

## 6. 完成标准

- `node mango-ui/packages/create-mango-app/src/index.mjs --help` 可执行。
- `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` 通过。
- Sprint 6 交付台账检查通过。

## 7. 遗留问题

- 在线 Initializr 服务、交互式选项、Nexus 配置选择和持续 `mango add` 生成器后续 Sprint 处理。
