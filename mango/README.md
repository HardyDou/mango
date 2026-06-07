# Mango 后端底座

Java Spring Cloud Alibaba 微服务开发底座。

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.x + Spring Cloud Alibaba |
| 数据库 | MyBatis-Plus + Flyway Migration |
| KV 存储 | Redis/db/memory（可切换） |
| 注册/配置 | Nacos |
| 分布式事务 | Seata |
| 国密算法 | SM2/SM3/SM4 |

## 核心规范

| 序号 | 规范 | 说明 |
|------|------|------|
| 01 | `01-code.md` | 代码规范（C1-C5） |
| 02 | `02-naming.md` | 命名规范 |
| 03 | `03-api.md` | API 设计 |
| 04 | `04-db.md` | 数据库规范 |
| 05 | `05-module.md` | 模块分层（SPI + Starter） |
| 06 | `06-security.md` | 安全规范 |
| 07 | `07-persistence.md` | 事务规范 |
| 08 | `08-test.md` | 测试规范 |
| 09 | `09-ui.md` | UI 组件 |
| 10 | `10-dev-flow.md` | 开发流程 |

### 注释与文档

- 新增和修改的代码注释、JavaDoc、README、设计文档、交付记录默认使用中文。
- 新增 Java 类型如需 `@author`，作者值使用当前系统用户；Mango 代码生成器自动提取系统用户。
- 不再手写固定作者名；历史代码可在触达时逐步清理。

## 常用命令

```bash
# 代码检查
mvn mango:check

# 生成模块
mvn mango:gen-module -Dname=xxx

# 生成 CRUD
mvn mango:gen-crud -Dmodule=xxx -Dentity=User -Dtable=usr_user

# 检查迁移脚本是否包含审计字段和租户字段
mvn mango:check -Drule=persistence-schema

# 禁止 HTTP 路径参数
mvn mango:check -Drule=path-param

# 检查 PERMISSION 接口必须声明权限码
mvn mango:check -Drule=permission-param
```

## 模块结构

```
mango/
├── mango-app/                   # 应用层
│   └── mango-admin-app/         # 管理后台装配应用
├── mango-common/               # 公共代码
├── mango-infra/                # 基础设施
│   ├── mango-infra-context/     # 上下文
│   ├── mango-infra-crypto/      # 国密算法
│   ├── mango-infra-persistence/          # 关系型持久化基础设施
│   ├── mango-infra-doc/         # OpenAPI 文档
│   ├── mango-infra-feign/       # OpenFeign
│   ├── mango-infra-kv/          # KV 存储抽象
│   ├── mango-infra-log/         # 日志配置
│   ├── mango-infra-module/      # 模块元数据与部署映射
│   ├── mango-infra-realtime/   # 客户端消息通信，含 SSE/WebSocket adapter
│   ├── mango-infra-test/        # 基础设施测试支撑
│   └── mango-infra-web/         # Web 封装
├── mango-platform/             # 平台能力
│   ├── mango-access/           # 边界入口
│   ├── mango-auth/             # 认证
│   ├── mango-authorization/    # 授权与安全基础适配
│   ├── mango-identity/         # 账号、身份资料与认证用户事实
│   ├── mango-captcha/          # 验证码
│   ├── mango-biz-notification/          # 消息
│   ├── mango-job/             # 任务调度与任务管理
│   ├── mango-org/              # 组织
│   └── mango-system/           # 系统配置、字典、租户、日志、路由、区域、国际化
├── mango-extension/            # 可选扩展
│   └── mango-ai/               # AI 能力
├── mango-parent/               # 父 POM
└── mango-tools/               # Maven 插件 & 代码规则
    └── mango-maven-plugin/    # Mango CLI 插件（含模板和规则）
```

### 层次结构

| 层级 | 模块 | 说明 |
|------|------|------|
| 应用层 | mango-app | 部署单元 |
| 平台能力层 | mango-platform | 通用业务能力 |
| 基础设施层 | mango-infra | 技术组件 |
| 公共层 | mango-common | 公共代码 |

## SPI + Starter 机制

每个业务域包含 4 个子模块：

- `mango-xxx-api` - 接口定义
- `mango-xxx-core` - 核心实现
- `mango-xxx-starter` - 本地调用
- `mango-xxx-starter-remote` - 远程调用

详情参见 [05-module.md](./.claude/rules/05-module.md)

## 平台能力文档

- [任务管理使用说明](./mango-platform/mango-job/README.md)
