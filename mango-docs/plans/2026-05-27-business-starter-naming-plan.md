# Mango Business Starter 命名收敛计划

## 1. 目标

将业务项目生产起点统一命名为 `mango-business-starter`，将本地初始化 CLI 统一命名为 `create-mango-app`，并补充业务项目开发说明。

## 2. 范围

- 重命名仓内业务项目 starter 资产。
- 重命名初始化 CLI 包路径、包名、bin 和说明文档。
- 更新 Issue #26 相关计划和台账中的当前路径与命令。
- 新增业务项目开发说明，说明生成项目结构、依赖边界、PMO 继承、前后端协作和验证入口。
- 在 business starter 中带出必要 Mango PMO baseline，使外部业务仓可以独立执行 preflight 和台账检查。

## 3. 不做什么

- 不实现 Mango Initializr Web 服务。
- 不新增交互式 CLI。
- 不改变 `mango-admin-starter`、`@mango/admin-shell` 等框架运行时能力。
- 不启动完整前后端服务做 E2E。

## 4. 设计说明

### 4.1 影响模块

- `mango-business-starter`
- `mango-ui/packages/create-mango-app`
- `mango-docs/designs/business-project-development-guide.md`
- `mango-docs/plans`
- `mango-business-starter/business-pmo/mango-baseline`

### 4.2 接口变化

无 HTTP API 变化。CLI 对外入口调整为：

```bash
npm create mango-app@latest <project>
mango init <project>
```

### 4.3 数据变化

无数据库变化。

### 4.4 菜单/页面/权限变化

无 Mango 运行态菜单、页面和权限数据变化。starter 内的资源清单模板路径随目录重命名继续保留。

### 4.5 测试范围

- starter 资产完整性检查。
- CLI 临时项目生成检查。
- 文档和台账 PMO 检查。
- 全仓旧命名引用扫描。
- starter 内 Mango baseline preflight 检查。

## 5. 完成标准

- `mango-business-template` 不再作为当前资产路径出现。
- `create-mango-business` 不再作为当前 CLI 包路径出现。
- starter 和 CLI 自检通过。
- 本计划台账检查通过。
