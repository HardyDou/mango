# 编号生成 Numgen

## 1. 概览

`mango-numgen` 是 Mango 的业务编号生成能力，用来统一生成订单号、退款号、对账批次号、工单号、合同号等业务编号。

它对外提供两类能力：

- 业务取号：按 `genKey` 生成单个编号或批量编号。
- 编号规则管理：维护生成器、规则版本、规则片段、序列状态和生成历史。

业务侧使用时要注意两点：

- `numgen` 返回的是编号字符串，不负责保存业务单据。
- 业务表仍然必须给最终编号字段建唯一约束；`numgen` 不替代业务唯一性兜底，也不承诺编号连续无空洞。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 单个取号 | 根据 `genKey` 和动态参数返回一个编号 | `NumgenApi.nextValue` / `POST /numgen/next` |
| 批量取号 | 一次返回多个编号，`count` 范围为 1-1000 | `NumgenApi.batchValue` / `POST /numgen/batch` |
| 规则校验 | 保存或发布前校验规则片段是否可用 | `NumgenApi.validateRule` / `POST /numgen/rules/validate` |
| 生成器管理 | 维护业务编号键、名称、业务域和启停状态 | 管理接口 / 前端编号规则页面 |
| 规则版本管理 | 维护规则版本、发布状态和历史版本 | 管理接口 / 前端编号规则页面 |
| 规则片段管理 | 支持固定文本、日期、参数、序列和表达式片段 | 管理接口 / 前端编号规则页面 |
| 序列查询 | 查看当前序列值和分组范围 | 管理接口 / 前端编号规则页面 |
| 生成历史 | 查询生成结果、规则版本、业务键、输入摘要和失败原因 | 管理接口 / 前端编号规则页面 |

## 3. 后端接入

### 3.1 开发依赖

业务模块只需要面向 API 契约编码时，引入 `mango-numgen-api`：

```xml
<dependency>
    <groupId>io.mango.platform.numgen</groupId>
    <artifactId>mango-numgen-api</artifactId>
</dependency>
```

业务代码优先依赖 `NumgenApi`：

```java
import io.mango.numgen.api.NumgenApi;
import io.mango.numgen.api.command.NumgenNextCommand;

NumgenNextCommand command = new NumgenNextCommand();
command.setGenKey("ORDER_NO");
command.getParams().put("orgCode", "HQ");

String orderNo = numgenApi.nextValue(command).getData();
```

### 3.2 部署依赖

提供编号生成能力的应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.numgen</groupId>
    <artifactId>mango-numgen-starter</artifactId>
</dependency>
```

微服务中只远程消费编号能力的应用启用 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.numgen</groupId>
    <artifactId>mango-numgen-starter-remote</artifactId>
</dependency>
```

`mango-numgen-starter` 默认随应用启用；需要关闭时配置：

```yaml
mango:
  numgen:
    enabled: false
```

## 4. 前端接入

管理后台使用 `@mango/numgen`，它是 `admin-pages` 页面插件和前端 API 封装，不是官网、C 端页面或普通业务页面组件。

```ts
import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';

registerMangoNumgenAdminPages();
```

菜单打开页面时使用以下 component key：

```text
platform/numgen/index
numgen/index
```

业务前端如果必须直接取号，可使用 `@mango/numgen` 暴露的 `numgenApi`。更推荐由业务后端取号并和业务单据保存放在同一个业务流程里处理唯一约束、重试和幂等。

```ts
import { numgenApi } from '@mango/numgen';

const orderNo = await numgenApi.nextValue({
  genKey: 'ORDER_NO',
  params: { orgCode: 'HQ' },
});
```

## 5. 快速开始

1. 确认部署应用已启用 `mango-numgen-starter`，数据库 migration 已执行。
2. 在编号规则管理页创建生成器，填写 `genKey`、`genName`、`domainCode` 和启停状态。
3. 创建规则版本，按顺序配置规则片段。
4. 使用预览能力确认样例编号符合业务格式。
5. 发布规则版本。
6. 业务保存单据前调用 `NumgenApi.nextValue` 或 `NumgenApi.batchValue` 获取编号。
7. 业务表对编号字段建立唯一索引；唯一冲突时按业务策略重试或提示。

## 6. 配置说明

YAML 配置用于控制编号能力是否启用，以及规则缓存和序列分配锁的时间。

```yaml
mango:
  numgen:
    enabled: true
    kv:
      rule-cache-ttl-seconds: 300
      allocation-lock-ttl-seconds: 10
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.numgen.enabled` | `true` | 是否启用本应用内的编号生成 starter。 |
| `mango.numgen.kv.rule-cache-ttl-seconds` | `300` | 生效规则缓存秒数。规则发布后如果短时间内仍看到旧格式，先看这个 TTL。 |
| `mango.numgen.kv.allocation-lock-ttl-seconds` | `10` | 序列分配锁 TTL。高并发取号时需要确保 KV 后端可用。 |

## 8. 运行时配置字段

运行时规则在编号规则页面维护，核心字段如下。

### 8.1 生成器字段

| 字段 | 含义 | 约束 |
|------|------|------|
| `genKey` | 编号规则键，业务取号时传入 | 必填，最长 128 字符 |
| `genName` | 编号名称 | 必填，最长 128 字符 |
| `domainCode` | 业务域编码，例如 `PAYMENT` | 必填，最长 64 字符 |
| `status` | 生成器状态 | `1` 启用，`0` 停用 |

### 8.2 规则字段

| 字段 | 含义 | 约束 |
|------|------|------|
| `genKey` | 归属生成器 | 必填 |
| `ruleName` | 规则名称 | 必填，最长 128 字符 |
| `version` | 规则版本 | 未传时按后端规则处理 |
| `status` | 规则状态 | `1` 启用，`0` 停用 |
| `publishStatus` | 发布状态 | `1` 生效中，`0` 未生效 |

### 8.3 规则片段字段

| 字段 | 含义 | 约束 |
|------|------|------|
| `ruleId` | 归属规则 ID | 必填 |
| `sortOrder` | 片段顺序 | 必填，从 1 开始 |
| `segmentType` | 片段类型 | `TEXT`、`DATE`、`PARAM`、`SEQ`、`EXPR` |
| `segmentName` | 片段名称 | 必填，最长 128 字符 |
| `literalValue` | 固定文本或表达式文本，支持 `${参数ID}` 占位符 | 最长 128 字符 |
| `variableKey` | 参数片段读取的参数 key | 最长 128 字符 |
| `dateFormat` | 日期格式 | 最长 64 字符 |
| `seqWidth` | 序列宽度 | 1-20 |
| `padChar` | 序列补位字符 | 单字符 |
| `sequenceScope` | 是否参与流水分组 | `1` 参与，`0` 不参与 |

片段常用组合：

| 片段类型 | 常用字段 | 说明 |
|----------|----------|------|
| `TEXT` | `literalValue` | 固定前缀，例如 `PO`。 |
| `DATE` | `dateFormat`、`sequenceScope` | 日期片段，例如 `yyyyMMdd`；参与流水分组后可按日期重置序列。 |
| `PARAM` | `variableKey`、`sequenceScope` | 从取号请求 `params` 读取值。 |
| `SEQ` | `seqWidth`、`padChar` | 流水号，例如 8 位补零。 |
| `EXPR` | `literalValue` | 表达式文本，支持参数占位符。 |

## 9. 请求与返回字段

### 9.1 业务取号

| 方法 | 路径 | 入参 | 返回 |
|------|------|------|------|
| `POST` | `/numgen/next` | `genKey`、`params` | 单个编号字符串 |
| `POST` | `/numgen/batch` | `genKey`、`count`、`params` | 编号字符串数组 |
| `POST` | `/numgen/rules/validate` | `genKey`、`ruleName`、`segments` | 规则校验结果 |

`params` 是动态参数 Map，`PARAM` 片段和带占位符的文本片段会读取这里的值。

### 9.2 管理接口

| 能力 | 路径 |
|------|------|
| 生成器分页、详情、新增、修改、启停、删除 | `/numgen/generators/**` |
| 规则分页、详情、新增、修改、启停、删除、发布、预览 | `/numgen/rules/**` |
| 规则片段分页、详情、新增、修改、删除 | `/numgen/segments/**` |
| 序列分页查询 | `/numgen/sequences/page` |
| 历史分页查询 | `/numgen/histories/page` |

### 9.3 常用返回字段

| 返回对象 | 字段 | 含义 |
|----------|------|------|
| `NumgenGeneratorVO` | `id`、`genKey`、`genName`、`domainCode`、`status` | 生成器基础信息 |
| `NumgenGeneratorVO` | `currentRuleVersion`、`currentPublishStatus`、`hasUnpublishedChanges` | 当前发布版本和是否有未发布修改 |
| `NumgenRuleVO` | `id`、`genKey`、`ruleName`、`version`、`status`、`publishStatus`、`versionState` | 规则版本信息 |
| `NumgenRuleSegmentVO` | `sortOrder`、`segmentType`、`literalValue`、`variableKey`、`dateFormat`、`seqWidth`、`padChar`、`sequenceScope` | 规则片段配置 |
| `NumgenPreviewVO` | `genKey`、`ruleVersion`、`segments`、`values` | 预览片段和预览编号 |

## 10. 管理入口

授权基线会初始化编号规则菜单：

| 菜单 | 路由 | component | 权限码 |
|------|------|-----------|--------|
| 编号规则 | `/data/numgen` | `@/views/numgen/index.vue` | `numgen:manage:list` |
| 编号规则查询 | 无页面路由 | 无 | `numgen:manage:list` |
| 编号规则维护 | 无页面路由 | 无 | `numgen:manage:write` |

前端运行时注册的页面 key 是 `platform/numgen/index` 和 `numgen/index`。如果菜单可见但页面打不开，先检查前端是否注册 `@mango/numgen/admin-pages`，再检查菜单 component 与运行时页面 key 的映射。

## 11. 数据与初始化

编号生成表由 `mango-numgen-core/src/main/resources/db/migration/numgen` 初始化。

| 脚本 | 内容 |
|------|------|
| `V1__init_numgen.sql` | 创建 `numgen_generator`、`numgen_rule`、`numgen_rule_segment`、`numgen_sequence`、`numgen_history`。 |
| `V2__numgen_domain.sql` | 给生成器补充 `domain_code`，默认值为 `NUMGEN`。 |
| `V6__payment_number_generators.sql` | 初始化支付域编号生成器，租户为 `1`，编号格式为“前缀 + 日期 yyyyMMdd + 8 位日内序列”。 |

支付域内置 `genKey` 包括：

```text
PAY_BIZ_ORDER_NO
PAY_ORDER_NO
PAY_REFUND_ORDER_NO
PAY_BIZ_REFUND_NO
PAY_REFUND_APPROVAL_NO
PAY_FLOW_NO
PAY_REFUND_FLOW_NO
PAY_FEE_FLOW_NO
PAY_ADJUST_FLOW_NO
PAY_NOTIFY_NO
PAY_RECON_BATCH_NO
PAY_DIFF_NO
PAY_QUERY_NO
PAY_REFUND_QUERY_NO
PAY_EXCEPTION_NO
PAY_OFFLINE_COLLECTION_NO
PAY_OFFLINE_REFUND_NO
PAY_OFFLINE_BANK_BATCH_NO
PAY_MANGO_VIRTUAL_NO
PAY_MANGO_SCENARIO_NO
```

菜单、权限和前端模块运行策略由授权基线初始化，入口在 `mango-authorization-core/src/main/resources/db/migration/authorization/V1__init_authorization.sql`。

## 12. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 取号返回规则不存在 | `genKey` 是否正确，生成器和规则是否启用，规则是否已发布。 |
| `PARAM` 片段取不到值 | 取号请求 `params` 是否包含规则片段的 `variableKey`。 |
| 发布后仍是旧格式 | `mango.numgen.kv.rule-cache-ttl-seconds`，当前生效版本和发布状态。 |
| 编号重复 | 业务表唯一索引、是否绕过 `NumgenApi` 手工拼号、KV 后端是否可用。 |
| 编号不连续 | 这是允许结果；失败、重试、事务回滚和并发竞争都可能造成跳号。 |
| 每天没有重新从 1 开始 | 日期片段是否设置 `sequenceScope = 1`。 |
| 管理页面无入口 | 授权基线是否执行，角色是否拥有 `numgen:manage:list`，前端是否注册 `@mango/numgen/admin-pages`。 |

## 13. 相关文档

- [前端编号生成包](../../../mango-ui/packages/numgen/README.md)
- [支付模块](../mango-payment/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
