# Mango 固定 API_RESOURCE 与 MVC 自动扫描审计

审计范围：Mango 源码仓 `mango/**/META-INF/mango/resources/*` 固定资源声明，以及 `mango-authorization-resource-sync-starter` 的默认 MVC 扫描范围。

## 结果

- 固定 `API_RESOURCE` 文件：5 个
- 固定声明总数：27 个
- 具备 `handlerClass`/`handlerMethod` 且可对应当前 Controller/Endpoint 的声明：24 个
- 其中 24 个路径和 HTTP 方法同时存在于 `io.mango` 包下的 MVC HandlerMapping 候选范围内。
- API Resource Provider 默认启用：`mango.authorization.resource-sync.resource-provider.enabled=true`（`matchIfMissing=true`）。
- 默认扫描包：`io.mango`；默认排除项只有 `/error,/actuator/**`。

## 固定声明文件

| 模块 | 文件 | 固定声明数 | 当前可确认的自动扫描重叠 |
|---|---|---:|---:|
| auth | `auth-common-public-api.yml` | 11 | 11 |
| system | `system-common-public-api.yml` | 2 | 2 |
| information | `information-common-public-api.yml` | 5 | 2（另 3 条为通配路径且无当前 handler 元数据） |
| link | `link-common-public-api.yml` | 1 | 1 |
| cms | `cms-common-public-api.yml` | 8 | 8 |

## 结论

Mango 平台内也存在与 `baohan-system` 相同的声明来源重叠模式：固定 YAML 与 Controller 自动扫描同时声明同一 API。固定声明使用稳定资源 ID，自动扫描使用 `ApiResourceDeclarationConverter` 计算的资源 ID；如果两者生成相同 `resourceType + bizKey`，Resource Registry 会按 fail-closed 规则拒绝其中一条。

本审计未修改 Mango 平台行为，也未将该独立问题混入 Issue #1111。应单独评审以下方案之一：

1. 为固定 `API_RESOURCE` 声明提供平台级自动排除/唯一来源机制；或
2. 移除固定声明，统一使用 Controller 自动扫描；或
3. 保留固定声明，并为各应用显式维护 `exclude-paths`。

当前 Issue #1111 的 V5 迁移、测试、文档和 baohan-system 根因 Issue 已完成；该审计结果是后续独立治理项。
