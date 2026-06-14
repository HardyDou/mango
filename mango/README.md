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

## 规范入口

长期规范只维护在 `mango-pmo/rules/**`。后端开发先通过 PMO preflight 获取本次必读规则，再按能力地图定位模块 README。

- [后端代码规范](../mango-pmo/rules/backend/01-code.md)
- [后端 API 规范](../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../mango-pmo/rules/backend/06-security.md)
- [持久化规范](../mango-pmo/rules/backend/07-persistence.md)
- [后端测试规范](../mango-pmo/rules/backend/08-test.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)

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

# HTTP 路径参数检查
mvn mango:check -Drule=path-param

# PERMISSION 接口权限码声明检查
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

详情参见 [后端模块规范](../mango-pmo/rules/backend/05-module.md)。

## 能力说明

- [Mango 能力地图](../mango-docs/capabilities/README.md)
- [Admin Starter](./mango-admin-starter/README.md)
- [App 拓扑](./mango-app/README.md)
- [Common 公共契约](./mango-common/README.md)
- [Infra 能力目录](../mango-docs/capabilities/README.md#5-后端基础设施能力)
- [Platform 能力目录](../mango-docs/capabilities/README.md#4-后端平台能力)
