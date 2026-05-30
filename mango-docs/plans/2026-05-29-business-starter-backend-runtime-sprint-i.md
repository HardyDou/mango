# Business Starter 后端运行骨架 Sprint I

## 1. 背景

Sprint H 已证明 Mango 前端发布物可以通过 registry 被外部业务项目消费，但最终目标中的 `mvn test` 和 `./scripts/dev-start.sh` 仍缺少生成项目内的后端根 POM、单体 app 和统一启动入口。

## 2. 目标

让 `create-mango-app` 生成的业务项目具备可验证的后端 Maven 聚合结构、单体 Spring Boot app 和前后端启动脚本，使初始化项目从“前端可消费”推进到“具备后端启动骨架”。

## 3. 范围

- 为 `mango-business-starter` 增加 `backend/pom.xml`。
- 增加 `backend/apps/{{projectKebab}}-monolith-app` 单体 app 模块。
- 增加 `scripts/dev-start.sh`，统一读取 `.env` 后启动后端和前端。
- 增加 `scripts/dev-stop.sh`，按本项目 pid 文件停止本地进程。
- 更新模板检查和 Initializr CLI 检查。
- 同步 `create-mango-app` 内置模板。

## 4. 不做什么

- 不声明真实 Nexus 已发布 Mango 后端 Maven 依赖。
- 不补真实数据库初始化验收、真实登录、权限、租户和 CRUD 全链路。
- 不把当前业务 service 的占位 CRUD 行为声明为真实业务实现。

## 5. 设计说明

### 5.1 影响模块

- `mango-business-starter`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter`
- `mango-ui/packages/create-mango-app/scripts/check-cli.mjs`
- `mango-docs/plans`

### 5.2 接口变化

无 HTTP API 变化。新增生成项目脚本：

```bash
./scripts/dev-start.sh
./scripts/dev-stop.sh
```

### 5.3 数据变化

无数据库结构变化。新增单体 app 使用已有模块 migration 和 Mango 后端依赖完成后续 Flyway 初始化。

### 5.4 测试范围

- 新特性测试：模板检查、CLI 生成项目检查、脚本语法检查、生成项目 Maven validate。
- 回归测试：复跑 Sprint H registry E2E、包契约检查、前端生成项目类型检查/构建链路。
- 例外测试：真实数据库启动、真实登录权限链路、业务 CRUD 持久化仍不在本 Sprint 证明。

## 6. 完成标准

- `node mango-business-starter/scripts/check-template.mjs` 通过。
- `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` 通过。
- 生成项目包含后端根 POM、单体 app 和 `scripts/dev-start.sh`。
- 生成项目 `mvn -f backend/pom.xml validate` 和 `mvn -f backend/pom.xml test` 通过。
- 交付台账检查通过。

## 7. 验证结果

已通过 `/tmp/mango-sprint-i-generated/sprint-i-platform` 临时生成项目验证后端运行骨架：生成项目包含 `backend/pom.xml`、`backend/apps/sprint-i-platform-monolith-app`、`scripts/dev-start.sh` 和 `scripts/dev-stop.sh`，并通过 `mvn -f backend/pom.xml validate`、`mvn -f backend/pom.xml test`。

已通过 `pnpm package:registry-e2e -- --evidence-dir /tmp/mango-sprint-i-regression-evidence` 完成 Sprint H registry 外部消费回归，发布 15 个 `@mango/*` 前端包后生成外部项目，完成安装、类型检查、构建和浏览器冒烟。

## 8. 风险与限制

- 后端 Maven 依赖是否能从真实仓库解析，取决于 Mango 后端物料发布状态和本机/CI Maven settings。
- 真实启动还依赖本地 MySQL、数据库名、账号密码、Flyway 初始化、Office 插件开关等环境。
- 当前业务 starter 的业务 service 仍是骨架实现，不能作为真实 CRUD 完成依据。
