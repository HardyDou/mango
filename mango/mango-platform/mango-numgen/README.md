# 编号生成 Numgen

## 1. 概览
`mango-numgen` 提供租户内业务编号生成能力：维护编号生成器、规则版本、规则片段、序列和生成历史，并对外提供单个取号、批量取号、规则校验和规则预览。

主要使用者是订单、合同、支付、对账、退款、线下收款、工单等需要稳定业务编号的模块。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务需要统一生成单据号、订单号、流水号、批次号 | Maven 依赖 / HTTP API / Java API |
| 编号格式需要由管理端维护，例如前缀、日期、业务参数和流水号组合 | Maven 依赖 / HTTP API / Java API |
| 多实例并发取号时需要用 KV 锁和数据库唯一约束保证不重复 | Maven 依赖 / HTTP API / Java API |
| 需要记录每次编号生成历史，方便追踪生成规则和输入参数 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务需要统一生成单据号、订单号、流水号、批次号。
- 编号格式需要由管理端维护，例如前缀、日期、业务参数和流水号组合。
- 多实例并发取号时需要用 KV 锁和数据库唯一约束保证不重复。
- 需要记录每次编号生成历史，方便追踪生成规则和输入参数。

## 4. 边界说明
- 不替代业务表唯一约束；业务表仍必须对最终编号建唯一键。
- 不保证编号连续无空洞；失败、事务回滚或并发竞争都可能造成序列跳号。
- 不负责业务实体保存，只返回编号字符串。
- 不适合拿来生成密码、token、密钥等安全随机值。

## 5. 模块组成
- `mango-numgen-api`：`NumgenApi`、生成器、规则、片段、序列、历史 API 和 DTO。
- `mango-numgen-core`：规则渲染、序列分配、KV 锁、Mapper、历史记录和支付编号种子。
- `mango-numgen-starter`：注册 `NumgenAutoConfiguration` 和 HTTP Controller。
- `mango-numgen-starter-remote`：注册 `NumgenFeignClient`，供微服务远程取号。

调用方负责选择 `genKey`、传入规则所需参数，并在保存业务数据时用数据库唯一约束兜底。

## 6. 接入方式
提供编号服务的应用引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.numgen</groupId>
    <artifactId>mango-numgen-starter</artifactId>
</dependency>
```

只做远程消费的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.numgen</groupId>
    <artifactId>mango-numgen-starter-remote</artifactId>
</dependency>
```

业务代码优先注入 `NumgenApi`，调用 `nextValue` 或 `batchValue`。

## 7. 配置说明
配置前缀：`mango.numgen.kv`。

| 配置项 | 类型 | 默认值 | 含义 |
|--------|------|--------|------|
| `rule-cache-ttl-seconds` | long | `300` | 生效规则表达式缓存秒数。规则发布后要关注缓存刷新或等待 TTL。 |
| `allocation-lock-ttl-seconds` | long | `10` | 序列分配分布式锁 TTL，防止多实例并发分配同一序列。 |

配置示例：

```yaml
mango:
  numgen:
    kv:
      rule-cache-ttl-seconds: 300
      allocation-lock-ttl-seconds: 10
```

KV 能力来自 `mango-infra-kv`。如果业务高并发取号，先验收 KV 后端可用，再做并发压测。

## 8. API 与扩展
业务取号接口：

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/numgen/next` | 按 `genKey` 生成一个编号。 |
| POST | `/numgen/batch` | 批量生成编号。 |
| POST | `/numgen/rules/validate` | 校验规则片段是否合法。 |

管理接口：

| 根路径 | 用途 |
|--------|------|
| `/numgen/generators` | 生成器分页、详情、新增、修改、启停、删除。 |
| `/numgen/rules` | 规则分页、详情、新增、修改、启停、删除、发布、预览。 |
| `/numgen/segments` | 规则片段分页、详情、新增、修改、删除。 |
| `/numgen/sequences` | 序列分页查询。 |
| `/numgen/histories` | 生成历史分页查询。 |

规则片段支持：

| 片段类型 | 必填字段 | 含义 |
|----------|----------|------|
| `TEXT` | `literal_value` | 固定文本，支持 `${变量名}` 替换。 |
| `EXPR` | `literal_value` | 当前实现按文本表达式处理，同样支持 `${变量名}` 替换。 |
| `DATE` | `date_format` | 使用 Java `DateTimeFormatter` 格式化当前时间。 |
| `PARAM` | `variable_key` | 从取号请求参数读取值，缺失会失败。 |
| `SEQ` | `seq_width` | 序列号，按宽度和 `pad_char` 补齐。 |

`sequence_scope = 1` 的非 `SEQ` 片段会参与流水分组。例如日期片段参与分组时，每天独立递增。

## 9. 数据与初始化
Flyway 路径：`mango-numgen-core/src/main/resources/db/migration/numgen`。

核心表：

| 表 | 用途 |
|----|------|
| `numgen_generator` | 生成器定义，`tenant_id + gen_key + del_flag` 唯一。 |
| `numgen_rule` | 规则版本，记录生效版本和发布状态。 |
| `numgen_rule_segment` | 规则片段，按 `sort_order` 拼接。 |
| `numgen_sequence` | 当前序列值，`tenant_id + gen_key + rule_version` 唯一。 |
| `numgen_history` | 每次生成结果、规则版本、业务键、输入摘要和耗时。 |

初始化脚本：

- `V1__init_numgen.sql` 创建核心表并兼容早期模型字段。
- `V2__numgen_domain.sql` 给生成器补 `domain_code` 并默认写 `NUMGEN`。
- `V6__payment_number_generators.sql` 初始化支付域编号生成器，租户为 `1`，规则为“前缀 + 日期 yyyyMMdd + 8 位日内序号”。

支付域内置 `genKey` 包括 `PAY_ORDER_NO`、`PAY_REFUND_ORDER_NO`、`PAY_RECON_BATCH_NO`、`PAY_DIFF_NO`、`PAY_OFFLINE_COLLECTION_NO` 等，完整清单以 SQL 和 payment README 为准。

## 10. 管理入口
编号表都有 `tenant_id`。同一个 `genKey` 可在不同租户下存在不同规则和序列。

当前 numgen Controller 未声明细粒度 `@ApiAccess` 权限码；如果接入管理菜单，需要在 authorization 中为生成器、规则、片段、序列和历史页面配置菜单权限，并限制只有管理员能修改规则。业务取号接口通常由服务端调用，不应直接暴露给无权限前端。

## 11. 快速开始
1. 创建生成器，确定 `genKey` 和 `domainCode`。
2. 创建规则版本，配置 `TEXT`、`DATE`、`PARAM`、`SEQ` 等片段。
3. 调用规则预览，确认样例编号符合业务格式。
4. 发布规则。
5. 业务保存单据前调用 `NumgenApi.nextValue` 获取编号。
6. 保存业务表时对编号字段建唯一约束，失败时按业务策略重试或提示。

## 12. 问题排查
- 编号重复：先检查业务表唯一约束、KV 锁、租户上下文和是否绕过 `NumgenApi` 手工拼号。
- 编号不连续：这是允许的，不应把连续性作为业务正确性依据。
- 规则发布后格式没变：检查规则缓存 TTL、当前规则版本和发布状态。
- 缺少参数：`PARAM` 片段要求请求参数里存在对应 `variable_key`。
- 日期没有每日重置：确认日期片段设置了 `sequence_scope = 1`。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
