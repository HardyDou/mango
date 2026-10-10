# Resource Registry：把模块提供的初始化资源交给目标模块

## 先说人话

业务模块有一份“随模块发布的初始化资源”，例如菜单、字典、流程定义或系统配置。Resource Registry 不保存这些业务内容，也不替代目标模块；它只负责收集声明、调用目标模块的 `ResourceHandler`，并记录同步结果。

如果数据是用户运行期间创建或修改的业务数据，不要先把它写成 Resource 声明，应由业务 Owner Service 负责。

## 一个具体场景

一个支付模块要发布“支付订单号”规则：

1. 支付模块把规则声明放在 `META-INF/mango/resources/`。
2. Resource Registry 读取声明，按 `resource-type` 找到目标 handler。
3. `mango-numgen` 的 handler 把规则写入自己的目标表。
4. 业务开发者检查 registry、sync log 和目标表，而不是手工写 SQL。
5. 如果声明内容变化，递增 `version`，重新启动或显式同步。

最小流程：

```text
资源声明 -> Resource Registry -> ResourceHandler -> 目标表
                              └─失败 -> sync log -> 后续重试
```

## 最小接入路径

### 1. 先确认目标模块真的支持这个资源类型

Resource 常量不等于运行时已经开放。先看目标模块 README，或在当前应用读取：

```text
GET /resource/handler-specs
```

### 2. 选择声明方式

- 结构化菜单或较大资源：JSON；
- 少量配置：YAML；
- 需要代码生成声明：`ResourceProvider`。

正式声明默认放在：

```text
src/main/resources/META-INF/mango/resources/
```

### 3. 添加最小声明

声明至少包含 `schemaVersion`、`moduleCode`、`moduleName` 和 `declarations`。字段名以目标 handler 的 `ResourceHandlerSpec` 为准。下面是 README 中的 `SEQUENCE_RULE` 形态：

```yaml
mango:
  resource:
    schema-version: 1
    module-code: payment
    module-name: 支付
    declarations:
      SEQUENCE_RULE:
        - id: "2026061800600000002"
          version: 1
          biz-key: payment.numgen.pay-order-no
          name: 支付订单号
          target-module: numgen
          sync-mode: AUTO
          status: ACTIVE
          fields:
            generatorId: { type: LONG, value: 900000000002 }
            genKey: { type: STRING, value: PAY_ORDER_NO }
            genName: { type: STRING, value: 支付订单号 }
            domainCode: { type: STRING, value: PAYMENT }
```

### 4. 启动后确认结果

至少查看：

```text
/resource/registries/page
/resource/sync-logs/page
目标模块的目标表
```

确认目标模块的 handler 实际写入了目标数据。单独看到 registry 记录或历史 SUCCESS，不足以证明当前目标数据可用。

## 三个最容易混淆的词

| 词 | 直白解释 | 不代表什么 |
|---|---|---|
| Resource 声明 | 模块随制品提供的一份资源输入 | 不是业务运行数据表 |
| `ResourceHandler` | 目标模块把声明转换成自身数据的执行者 | 不是注册中心的数据库访问代理 |
| `RUNTIME_EVENTUAL` | Runtime 后台持续对账，不阻断流量 | 不是“启动前要完成”的 Bootstrap |

## 失败时先看哪里

| 现象 | 先检查 |
|---|---|
| 声明没有被读取 | 文件是否在 `META-INF/mango/resources/`，扩展名是否正确 |
| 资源冲突 | `id` 和 `resourceType + bizKey` 是否重复 |
| 目标表没有变化 | 当前应用是否装配目标 handler，`sync-mode` 是否符合预期 |
| 微服务没有上报 | `mango-resource-starter-remote`、`mango-resource-sync-starter` 和注册中心连接 |
| 持续重试 | `resourceStartupHealthIndicator`、sync log 和底层连接错误 |

## 权威来源

- [Resource README](../../../mango/mango-platform/mango-resource/README.md)
- [业务 Resource 重置与增量发布](../operations/resource-reset-incremental-release.md)
- [Mango 能力地图](../../capabilities/README.md)
