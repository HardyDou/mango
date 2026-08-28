# 业务 Resource 重置与增量发布

## 1. 适用场景

本文面向基于 Mango 开发业务模块的团队，说明业务代码、初始化数据和发布物料如何配合 Resource Registry。示例以“保函申请、保函类型和后台配置”这类业务为背景，但不绑定具体业务表。

本文是接入说明，不替代 PMO 规则。初始化数据的长期边界以 [数据库规范](../../../mango-pmo/rules/backend/04-db.md) 和 [Issue #184 数据治理设计](../../designs/2026-07-01-issue-184-data-governance-design.md) 为准。

## 2. 业务开发者先做数据分类

业务模块新增一张表或一条初始化数据时，先按下表归类。一个数据对象只能有一个主归属，不能同时由 Flyway、Resource 和后台初始化。

| 数据 | 归属 | 业务项目入口 | 发布行为 |
|---|---|---|---|
| 表、列、索引、约束 | Flyway | `<module>-core/src/main/resources/db/migration/<module>/V*.sql` | 已有库按 history 增量执行；空库可消费构建物 B baseline |
| 菜单、按钮权限、接口访问模式 | Resource Registry | `META-INF/mango/resources/<module>-common-menu.json` 或 typed declaration | 按模块 hash 和 Resource hash 增量协调 |
| 必须存在的字典、系统配置、编号规则、流程定义 | Resource Registry | `META-INF/mango/resources/` | 根据 `sync-mode` 处理；可运营数据优先 `INIT_ONLY` |
| 演示租户、演示保函、测试账号、样例流程 | Demo Resource / 测试 fixture | `META-INF/mango/demo/` 或测试目录 | 默认不加载，显式开启 demo 后才初始化 |
| 用户运行期创建的保函、审批实例、附件关系 | 业务 API / 管理后台 | Service、Controller、后台页面 | 不进入发布包，不由 Resource 覆盖 |
| 随版本发布的固定模板或文件 | `FILE_ASSET` Resource | `META-INF/mango/assets/` 或受控 `asset:` 根目录 | 按 SHA-256 物化到配置的文件存储 |
| 大字典、外部修复 SQL、停机数据修复 | 外部 Flyway upgrade | `${MANGO_HOME:-/opt/mango}/upgrade/<module>` | 按独立升级窗口和脚本发布 |

典型保函模块的“保函申请记录”和“审批实例”属于运行期业务数据；“保函类型字典”“默认编号规则”“后台菜单”才可能是正式 Resource。不要把业务 CRUD demo 行写进默认 Flyway，也不要把生产客户数据打进 Resource manifest。

## 3. 业务模块接入

### 3.1 单体

单体 app 依赖业务 `<module>-starter`，由同一个 Spring Boot app 执行业务 Flyway、扫描 Resource declaration 并调用本地 Handler。业务开发者需要确认：

1. `application.yml` 中 `<module>.enabled: true`。
2. 业务表 migration 位于本模块 `core` 的 migration 目录。
3. 菜单和权限声明位于业务 starter 的 `META-INF/mango/resources/`。
4. 菜单 `component` 与前端页面注册 key 完全一致。
5. 业务表的新增、查询、更新、删除都经过租户和权限校验。

### 3.2 微服务

提供方依赖 `<module>-starter` 并拥有业务表和 Resource 声明；调用方只依赖 `<module>-starter-remote` 或 API。调用方不读取提供方表，也不把提供方 migration 复制到自己的服务。

Resource 上报、目标模块和租户上下文必须能在部署环境中回读。网关、Feign 和服务间调用的认证头、租户 ID、trace 信息缺失时，先修复链路，不把 Resource 同步改成绕过权限的内部 SQL。

## 4. Resource declaration 约定

每条声明保持稳定 `id`、`bizKey` 和 `targetModule`。内容变化时递增 `version`，但不要因为一次发布重生成 ID。

```yaml
mango:
  resource:
    schema-version: 1
    module-code: guarantee
    module-name: 保函
    declarations:
      SYSTEM_CONFIG:
        - id: "6100000000000000001"
          version: 2
          biz-key: guarantee.application.expire-days
          name: 保函申请有效期
          target-module: system
          sync-mode: INIT_ONLY
          status: ACTIVE
          fields:
            configKey: { type: STRING, value: guarantee.application.expire-days }
            configValue: { type: STRING, value: "365" }
```

跨模块业务关系用固定身份解析，例如 `resourceId`、`code`、`bizCode` 或 `resourceType + bizKey`。不要用 jar 扫描顺序、文件顺序、模块启动顺序或目标表自增 ID 表达业务关系。现有 `moduleDependencies()` 与 `dependsOnResourceTypes()` 只作为存量协调兼容能力，不能成为新业务数据正确性的前提。

## 5. 两种发布模式

### 5.1 重置发布

适用对象是真正空库或明确允许销毁重建的开发/演示库。业务开发者按以下顺序操作：

1. 构建阶段生成并校验 Flyway B baseline、Resource manifest、文件 manifest 和应用制品。
2. 部署前确认数据库为空、数据库版本和字符集满足发布清单。
3. 执行 Bootstrap `plan -> apply -> verify -> finalize`，记录 generation、manifest fingerprint 和 receipt。
4. 显式开启 demo 时再加载 `META-INF/mango/demo/`，否则只加载正式资源。
5. 回读业务表、Resource Registry、菜单权限、流程定义和文件对象，再启动 Runtime。

当前版本的限制：Flyway cold baseline 已支持，但完整的非环境 Resource 数据库基线尚未落地。因此空库首次仍会执行 `RESOURCE_REQUIRED` 和对应 Handler；不能把当前重置发布宣称为“首次零 Handler”。

### 5.2 增量发布

适用对象是保留业务数据的已有库：

1. 先按 Flyway history 执行未执行的业务 migration，不消费 B baseline 覆盖已有库。
2. Bootstrap 读取当前 release manifest 和环境 receipt。
3. 模块 hash 不变时，整个模块跳过，不解析内部声明，不写 Registry 或同步日志。
4. 模块 hash 变化时，只协调模块内 hash 或状态发生变化的 Resource。
5. `FINALIZE` 只处理变化模块内、Registry 明确拥有的 `AUTO` 缺失资源。
6. 失败不推进 receipt；修复后用同一 generation 重试。

`SYSTEM_CONFIG` 当前支持 `updated_at` 退避：Handler 写入时使用固定同步时间并同步 `resource_registry.last_sync_time`；如果后台修改使目标行的 `updated_at` 与上次同步时间不一致，则返回 `PRESERVED`，保留数据库值，不推进该 Resource 的 source hash 和同步时间。同模块其它未修改 Resource 仍可继续发布。

这不是所有业务表的通用猜测机制。没有可靠 `updated_at`，或一个 Resource 管理多表/多行时，业务 Handler 需要自己提供受管状态判断；在此之前使用 `INIT_ONLY`、`MANUAL` 或 `LOCKED`，不要假装具备后台修改保护。

## 6. 保函类模块最小验收矩阵

| 用例 | 操作 | 关键断言 |
|---|---|---|
| 空库正式初始化 | 新建空库，关闭 demo，执行 Bootstrap | Flyway 表存在，正式菜单/配置/流程声明可见；当前版本允许首次 `RESOURCE_REQUIRED` |
| 空库演示初始化 | 新建隔离库，开启 demo | 演示租户和样例数据存在；关闭 demo 的库没有演示业务行 |
| 无变化重启 | 相同 release/generation 重启 | 模块 receipt 命中，Handler、Registry、sync log 和业务表写入为 0 |
| 单 Resource 变化 | 只修改保函有效期声明 | 只有对应 Resource 产生 `APPLIED`，同模块其它未变化资源不写 |
| 后台修改退避 | 管理后台把有效期改为 `730`，再发布声明 `367` | 数据库仍为 `730`，同步日志为 `PRESERVED`，hash/time 不推进 |
| 删除隔离 | 删除一个模块声明并 finalize | 只处理该变化模块的 Registry-owned `AUTO` 资源，不删除运行期保函或 `INIT_ONLY` 数据 |
| 中断恢复 | Handler 中途失败后重试 | 已成功模块 receipt 保留，失败模块可重试，不重复创建业务键 |
| 租户与权限 | 用两个租户和不同角色回读菜单、接口和保函数据 | 菜单、接口权限和业务数据均按租户/角色隔离，不能只凭前端隐藏断言 |
| 微服务消费 | 提供方和调用方分开启动 | 调用方通过 API/remote starter 工作，不访问提供方 core 或数据库 |

每个用例使用独立数据库或唯一测试前缀，并在结束后清理本用例创建的数据。证据至少包含 Bootstrap receipt、`resource_registry`、`resource_sync_log`、Flyway history、目标业务表和权限/租户回读。

## 7. 当前未覆盖的业务开发边界

- 完整 Resource cold baseline 和首次跳过 `RESOURCE_REQUIRED` 尚未实现。
- 不是所有 Handler 都支持 `updated_at` 退避；当前只有 `SYSTEM_CONFIG` 已接入通用判断。
- 旧 Handler 仍可能使用 `requiresCompleteBatch()` 兼容行为，业务模块升级前要确认其是否真正支持 changed-only 写入。
- 本地 MinIO 只代表 S3-compatible 语义，不等于 OSS、COS、Kodo 的 IAM、TLS、区域和限流验收。
- Resource Registry 不替代业务运行时 API，也不负责把生产业务数据复制到新环境。

排查顺序建议为：先看 Flyway history，再看 Bootstrap receipt 和模块 hash，然后看 Resource sync log/registry，最后看目标业务表、权限和租户数据。不要用手工 SQL 把 receipt 改成成功状态来绕过失败。

## 8. 相关入口

- [Resource Registry README](../../../mango/mango-platform/mango-resource/README.md)
- [业务 Starter README](../../../mango-business-starter/README.md)
- [单体拓扑](../../../mango-business-starter/topologies/monolith/README.md)
- [微服务拓扑](../../../mango-business-starter/topologies/microservice/README.md)
- [业务 API 构建期 cold baseline](./build-time-cold-baseline.md)
- [CI/CD 发布实践](./ci-cd-release-practices.md)
