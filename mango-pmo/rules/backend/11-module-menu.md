# 模块菜单初始化规范

## 1. 归口

- 功能模块自带的菜单、页面路由和按钮权限，归属功能模块自身。
- 授权模块只维护授权基础资产、菜单包、角色和资源清单消费能力。
- 功能模块禁止为自身菜单或按钮权限新增 `mango-authorization` migration。

## 2. 初始化方式

- 本地 `starter` 如需发布菜单，必须在 jar 内提供 `META-INF/mango/resource-manifest.json` 或 `META-INF/mango/resource-manifests/*.json`。
- 资源清单由 `mango-authorization-resource-sync-starter` 读取并注册。
- 资源清单以 `appCode + moduleCode + menuCode` 作为菜单幂等键。
- 资源清单可以引用已存在的 `packageCodes` 和 `roleCodes`，用于把菜单加入套餐或默认授权给角色。
- 资源清单不得创建菜单包、角色、主体或账号。

## 3. 前端一致性

- 菜单 `path` 必须匹配前端路由。
- 菜单 `component` 必须匹配前端页面注册键。
- 按钮权限码必须匹配前端真实权限点和后端接口访问策略。

## 4. 禁止事项

- 禁止把功能模块菜单初始化 SQL 写入非归属模块。
- 禁止用 README、设计文档或 evidence 代替资源清单。
- 禁止让资源清单覆盖或删除清单中不存在的历史授权资产。
