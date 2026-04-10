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

## 常用命令

```bash
# 代码检查
mvn mango:check

# 生成模块
mvn mango:gen-module -Dname=xxx

# 生成 CRUD
mvn mango:gen-crud -Dmodule=xxx -Dentity=User -Dtable=usr_user
```

## 模块结构

```
mango/
├── mango-admin-app/           # 管理后台聚合服务
├── mango-auth/                # 认证服务
├── mango-gateway/             # 网关
├── mango-infra/               # 基础设施
│   ├── mango-infra-crypto     # 国密算法
│   ├── mango-infra-security   # 权限安全
│   └── mango-infra-dal       # 数据访问抽象
├── mango-rbac/                # 权限管理
├── mango-system/              # 系统管理
└── mango-common/              # 公共模块
```

## SPI + Starter 机制

每个业务域包含 4 个子模块：

- `mango-xxx-api` - 接口定义
- `mango-xxx-core` - 核心实现
- `mango-xxx-starter` - 本地调用
- `mango-xxx-starter-remote` - 远程调用

详情参见 [05-module.md](./.claude/rules/05-module.md)
