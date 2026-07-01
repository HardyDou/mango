# Issue #184 S1 Resource Demo 隔离计划

状态：已完成。

当前使用入口见 [mango-resource README](../../mango/mango-platform/mango-resource/README.md) 和 [Issue #184 总设计](../designs/2026-07-01-issue-184-data-governance-design.md)。

## 目标

让 Resource 能区分正式资源和 demo 资源。正式资源继续默认扫描 `META-INF/mango/resources/`，demo 资源放入 `META-INF/mango/demo/`，只有显式启用后才会扫描。

## 范围

- `mango-resource-support` 增加 demo 扫描开关和默认 demo 路径。
- `ResourceDeclarationLoader` 在开关启用时追加 demo 资源路径。
- `mango-resource-core` 补充 loader 单元测试。

## 不做

- 不实现 DataOps 任务编排。
- 不引入任务历史表。
- 不迁移 CMS 现有数据。
- 不处理大 SQL、远程 URL SQL、磁盘文件导入。

## 方案

新增配置：

```yaml
mango:
  resource:
    registry:
      demo-enabled: false
      demo-locations:
        - classpath*:META-INF/mango/demo/*.json
        - classpath*:META-INF/mango/demo/*.yml
        - classpath*:META-INF/mango/demo/*.yaml
```

默认 `demo-enabled=false`，因此生产或普通启动不会读 demo 资源。需要演示数据的最终应用显式打开该开关；不通过更换依赖包、单独 demo starter 或环境目录实现。

## 验收计划

- 默认配置只加载 `META-INF/mango/resources/`，不会加载 `META-INF/mango/demo/`。
- `demo-enabled=true` 后可以加载 demo 声明。
- 现有 JSON/YAML 正式资源加载行为不变。
- Maven 定向测试通过。
