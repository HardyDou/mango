# Business Starter 真实启动 E2E Sprint J

## 1. 背景

Sprint I 已让 `create-mango-app` 生成项目具备后端 Maven 根、单体 app 和本地启动脚本，但还没有证明生成项目可在真实数据库上通过 `./scripts/dev-start.sh` 启动，并完成后端健康、业务 API 持久化和前端浏览器访问验收。

## 2. 目标

让生成的业务项目从“结构可编译”推进到“真实 dev-start 可启动、真实数据库可初始化、最小业务 CRUD 可持久化、前端页面可被浏览器 E2E 验收”。

## 3. 范围

- 升级 `mango-business-starter` 后端业务骨架，去除固定返回值，改为 MyBatis-Plus 持久化实现。
- 为业务 starter 增加 Mapper、实体表映射、事务边界和真实查询。
- 升级 `scripts/dev-start.sh`，支持 `.env` 配置、数据库自动创建、后端健康等待、前端可访问等待和 pid/log 管理。
- 增加生成项目真实启动 E2E 脚本，覆盖 CLI 生成、依赖安装、Maven 测试、数据库创建、`dev-start` 启动、HTTP API 验证、浏览器 E2E 截图和清理。
- 同步 `create-mango-app` 内置模板。

## 4. 不做什么

- 不声明完整登录、租户、菜单权限初始化链路已完成。
- 不声明所有 Mango 后端模块均已发布到远程 Maven 仓库。
- 不扩展复杂业务字段、审批流、文件上传等完整业务域能力。

## 5. 设计说明

### 5.1 影响模块

- `mango-business-starter`
- `mango-ui/packages/create-mango-app`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter`
- `mango-docs/plans`

### 5.2 接口变化

已有业务 API 语义由骨架返回升级为真实数据源：

- `POST /{module}/{{aggregateKebab}}s`：写入数据库并返回真实生成 ID。
- `GET /{module}/{{aggregateKebab}}s`：从数据库分页查询。
- `GET /{module}/{{aggregateKebab}}s/detail?id=...`：从数据库查询详情，不存在时返回业务错误。

启动脚本保持入口不变：

```bash
./scripts/dev-start.sh
./scripts/dev-stop.sh
```

### 5.3 数据变化

沿用 starter 已有 Flyway migration 创建业务表。实体增加 MyBatis-Plus `@TableName`、`@TableId(type = ASSIGN_ID)` 映射，业务 service 通过 Mapper 读写该表。

### 5.4 菜单/页面/权限变化

不新增菜单与权限初始化数据。前端 E2E 只验证生成项目基座、首页和业务菜单/业务页面加载，不声明真实登录权限链路完成。

### 5.5 测试范围

- 新特性测试：模板检查、CLI 生成检查、生成项目 Maven test、真实 `dev-start` E2E、真实 MySQL API 创建/分页/详情验证、浏览器访问和截图。
- 回归测试：复跑 registry publish/install/build/browser E2E，确认前端物料发布消费链路未回退。
- 负向/边界：验证不存在详情返回非 2xx 或错误响应；验证启动脚本端口占用前置检查和停服清理。

## 6. 完成标准

- `node mango-business-starter/scripts/check-template.mjs` 通过。
- `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` 通过。
- 生成项目 `mvn -f backend/pom.xml test` 通过。
- 生成项目 `./scripts/dev-start.sh` 可启动真实后端和前端，后端 `/actuator/health` 返回可用。
- E2E 通过真实 MySQL 验证业务 API 创建、分页和详情，不再返回固定 ID 或空列表。
- Playwright 浏览器 E2E 访问生成项目前端并产出截图证据。
- registry 消费 E2E 回归通过。

## 7. 风险与限制

- 真实启动依赖本机 MySQL、Maven 本地/远程依赖解析和 pnpm 可用。
- 认证、租户和菜单权限初始化仍未作为本 Sprint 完成项，后续需要单独 Sprint 验证。
- 如果 CI 没有 MySQL，需要为 E2E 提供等价容器化 MySQL 环境。
