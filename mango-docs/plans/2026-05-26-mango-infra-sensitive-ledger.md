# Mango Infra Sensitive 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| SENSITIVE-001 | 用户要求 | 迁移 Pigx sensitive 模块到 Mango infra | 新增 `mango-infra-sensitive` 聚合模块并拆分 `api/core/starter` | `mango/mango-infra/mango-infra-sensitive/pom.xml` | Maven 聚合构建 | DONE | `mango/mango-infra/mango-infra-sensitive` |
| SENSITIVE-002 | 用户要求 | VO 字段通过注解声明脱敏 | 在 api 提供 `@Sensitive`、`SensitiveType` 和脱敏控制上下文 | `mango-infra-sensitive-api` | 单元测试序列化注解行为 | DONE | `mango-infra-sensitive-api/src/main/java` |
| SENSITIVE-003 | 用户要求 | 脱敏不影响内部调用 | 脱敏只在 Jackson 输出边界执行，提供线程上下文和 SPI 绕过 | `SensitiveJacksonModule`、`SensitiveMaskingContext` | 单元测试验证原始对象值不变和绕过输出 | DONE | `mango-infra-sensitive-core/src/test/java` |
| SENSITIVE-004 | Pigx 源码 | 迁移 Pigx 固定脱敏类型和 URL 参数脱敏 | 在 core 提供手机号、身份证、邮箱、银行卡、密码、密钥、IPv4、车牌、URL 等策略 | `SensitiveMasker` | 单元测试覆盖主要策略 | DONE | `SensitiveMaskerTest` |
| SENSITIVE-005 | 用户要求 | 配置 JSON 按类型使用表单但底层存 JSON，需要 JSON 字段脱敏 | 增加 `SensitiveType.JSON` 和 `keys/fuzzy` 参数，递归处理 JSON key | `SensitiveJsonMasker` | 单元测试覆盖嵌套 JSON 和模糊 key | DONE | `SensitiveJacksonModuleTest` |
| SENSITIVE-006 | Pigx 源码 | 迁移敏感词 houbb 集成能力 | 用 `ISensitiveWordProvider` 取代 Pigx 固定远程 Feign 客户端 | `SensitiveWordCustomizer`、starter 自动配置 | 单元测试覆盖 provider 聚合 | DONE | `SensitiveWordCustomizerTest` |
| SENSITIVE-007 | Mango 模块规范 | 自动装配符合 Mango starter 边界 | starter 注册 runtime 服务、Jackson module、houbb bean 和 SPI 扩展点 | `SensitiveAutoConfiguration` | Maven 测试和 starter 编译 | DONE | `mango-infra-sensitive-starter` |
| SENSITIVE-008 | Mango 验证规范 | 新增代码必须有对应验证 | 补 core 行为测试并运行 Maven 测试、台账校验 | `src/test/java` 和验证命令 | `mvn test`、delivery contract check | DONE | 本台账验证记录 |
| SENSITIVE-009 | 用户要求 | 基础脱敏能力不得直接绑定具体安全框架和 Redis | sensitive 只定义 `ISensitiveRawAccessProvider`，原文访问由 `mango-authorization-support` 转接 Mango 授权，动态词库由业务侧 SPI 接入 | `AuthorizationSensitiveRawAccessProvider`、`SensitiveAutoConfiguration` | 单元测试覆盖授权适配和 raw access provider 绕过 | DONE | `mango-authorization-support/src/test/java` |
