# Issue #184 S2 Resource 同步策略计划

## 背景

Resource 已经有 `sync-mode`、版本、hash、registry、同步日志和变更日志。当前 `AUTO` 会在声明变化时覆盖目标数据，`MANUAL`/`LOCKED` 需要运行时人工接管，不适合模块在声明里直接表达“只初始化一次，后续升级不覆盖运行时改动”。

## 目标

在现有 Resource 能力内补齐 `INIT_ONLY` 同步策略：

- 首次同步时创建目标数据。
- 目标数据已存在后，模块升级只记录 registry 声明元数据，不调用目标 handler 覆盖业务表。
- `AUTO`、`MANUAL`、`LOCKED` 兼容现有行为。

## 范围

- `mango-resource-api` 增加 `ResourceSyncMode.INIT_ONLY`。
- `mango-resource-core` 同步服务识别 active + `INIT_ONLY` 的已有资源，保留目标表。
- `mango-resource-core` 补充集成测试。
- `mango-resource` README 补充使用说明。

## 不做

- 不新增 DataOps task/history 表。
- 不处理大 SQL、远程 URL SQL、磁盘文件导入。
- 不实现复杂系统升级脚本。
- 不修改目标模块 handler 的业务语义。

## 验收计划

- `INIT_ONLY` 资源首次同步会创建 registry 和目标数据。
- `INIT_ONLY` 资源声明升级后，registry 版本和 hash 更新，但目标业务表保留运行时改动。
- `AUTO` 资源声明升级仍覆盖目标数据。
- YAML/JSON 声明支持 `INIT_ONLY`，并兼容 `init-only`/`init_only` 写法。
- Resource 相关 Maven 测试通过。
