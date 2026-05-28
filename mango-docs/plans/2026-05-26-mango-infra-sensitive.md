# Mango Infra Sensitive 迁移设计说明

## 1. 目标

将 Pigx `pigx-common-sensitive` 迁移为 Mango 基础设施能力 `mango-infra-sensitive`，提供字段注解脱敏、JSON 配置脱敏、敏感词库扩展和 Spring Boot 自动装配。

## 2. 范围

- 新增 `mango/mango-infra/mango-infra-sensitive` 聚合模块。
- 按 Mango 模块边界拆分为 `api`、`core`、`starter`。
- 迁移 Pigx 字符串脱敏类型、URL 参数脱敏、敏感词 houbb 集成能力。
- 增加 JSON 字段脱敏能力，用于配置 JSON 等场景。
- 提供运行时绕过脱敏的上下文控制，保证内部调用可以显式获取原文。
- 纳入 Maven 聚合和依赖管理。
- 补充模块内单元测试。

## 3. 不做什么

- 不新增数据库表和 migration。
- 不新增菜单、页面、HTTP Controller 或权限资源。
- 不迁移 Pigx 固定 UPMS Feign 客户端和固定服务名，改为 Mango SPI 由业务侧提供词库。
- 不在对象转换阶段修改原始 VO 字段值，脱敏只发生在 JSON 输出边界。
- 不在 `mango-infra-sensitive` 直接依赖具体安全框架或具体 KV 实现；原文访问由 Mango 授权支撑层适配，词库刷新由业务侧 SPI 接入。

## 4. 设计输入

- 用户要求：完整迁移 `~/work/pigx/pigx/pigx-common/pigx-common-sensitive` 到 `mango-infra-sensitive`。
- 用户要求：脱敏是通用能力，业务只在 VO 字段添加注解；不能影响内部调用，部分场景需要原文。
- 用户要求：基础脱敏能力不得直接绑定具体安全框架和 Redis，需优先使用 Mango 自有授权与 KV 抽象边界。
- Pigx 源码：`/Users/hardy/work/pigx/pigx/pigx-common/pigx-common-sensitive`。
- PMO 规范：`mango-pmo/rules/backend/05-module.md`、`mango-pmo/rules/backend/08-test.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-infra/pom.xml`：新增 infra 聚合模块。
- `mango/pom.xml`：新增 Mango sensitive 三个 artifact 和 houbb sensitive-word 版本管理。
- `mango/mango-infra/mango-infra-sensitive`：新增脱敏能力模块。

### 5.2 接口变化

- 新增 `@Sensitive` 注解，支持固定类型、自定义保留位、JSON key 脱敏、模糊 key 匹配。
- 新增 `SensitiveType` 枚举，覆盖 Pigx 原类型并增加 `JSON`。
- 新增 `ISensitiveMaskingService`，用于判断当前输出是否需要脱敏。
- 新增 `ISensitiveRawAccessProvider`，由上层安全能力判断当前调用方是否允许查看原文。
- 新增 `ISensitiveWordProvider`，用于由业务模块提供敏感词白名单和黑名单。
- 新增 `SensitiveMaskingContext`，用于当前线程内显式关闭输出脱敏。

### 5.3 数据变化

无数据库变更。

### 5.4 菜单/页面/权限变化

无菜单、页面和权限资源变更。当前调用方是否可查看原文由 `mango-authorization-support` 适配 `ISensitiveRawAccessProvider` 后按配置的 authority 判断。

### 5.5 测试范围

- 固定脱敏策略：手机号、身份证、邮箱、密钥、IPv4、URL 参数。
- JSON key 脱敏：精确匹配、模糊匹配、嵌套对象。
- Jackson 输出边界脱敏：序列化结果脱敏，原始对象值不变。
- 显式绕过脱敏：`SensitiveMaskingContext` 和 `ISensitiveMaskingService` 均可返回原文。
- 敏感词定制器：聚合多个 `ISensitiveWordProvider`。

## 6. 风险与限制

- Pigx 固定远程词库客户端不能直接迁移到 Mango 基础设施层，否则会形成基础能力依赖业务服务的问题；本次以 SPI 保留扩展点。
- JSON 脱敏要求字段值是合法 JSON 字符串；非法 JSON 会按密钥策略整体脱敏，避免明文泄露。
- 动态词库来源不在脱敏基础模块内直接绑定具体存储；接入方需要热更新时应通过 `ISensitiveWordProvider` 结合 Mango KV 或事件能力提供词库。
