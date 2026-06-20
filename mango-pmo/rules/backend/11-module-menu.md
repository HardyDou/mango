# 模块菜单初始化规范

## 1. 归口

- 功能模块自带的菜单、页面路由和按钮权限，归属功能模块自身。
- 授权模块只维护授权基础资产、菜单包、角色和资源清单消费能力。
- 功能模块禁止为自身菜单或按钮权限新增 `mango-authorization` migration。

## 2. 初始化方式

- 功能模块 `starter` 如需发布菜单，必须在 jar 内提供 Resource Registry 声明文件：`META-INF/mango/resources/{module}-common-menu.{json,yml,yaml}`。
- 菜单资源类型固定为 `AUTH_MENU`，由 `mango-authorization` 提供 `ResourceHandler` 消费。
- 一个模块的菜单优先声明为一个 `AUTH_MENU` 资源，`menus` 字段保存菜单树；菜单量大时优先使用 JSON，避免低信息密度配置。
- 菜单资源以 `appCode + moduleCode + menuCode` 作为菜单幂等键。
- 菜单资源可以引用已存在的 `packageCodes` 和 `roleCodes`，用于把菜单加入套餐或默认授权给角色。
- 菜单资源不得创建菜单包、角色、主体或账号。
- 菜单 `parentCode` 必须能解析到同一应用下已存在或本次声明的父菜单；父级缺失必须失败，禁止静默挂到一级菜单。
- 旧 `META-INF/mango/resource-manifest.json` 和 `META-INF/mango/resource-manifests/*.json` 只作为存量迁移对象，不得作为新增菜单标准。

## 3. 前端一致性

- 菜单 `path` 必须匹配前端路由。
- 菜单 `component` 必须匹配前端页面注册键。
- 按钮权限码必须匹配前端真实权限点和后端接口访问策略。

## 4. 禁止事项

- 禁止把功能模块菜单初始化 SQL 写入非归属模块。
- 禁止用 Flyway SQL 维护菜单、按钮权限、菜单运行时配置、套餐授权和默认角色授权数据。
- 禁止用 README、设计文档或 evidence 代替资源清单。
- 禁止让资源清单覆盖或删除清单中不存在的历史授权资产。

## 5. 验收要求

- 菜单迁移必须按模块逐个处理。
- 每迁移一个模块，必须完成菜单树截图、菜单入口路径、关键页面打开结果、console/network 检查和用户人工确认。
- 未完成人工确认前，不得继续迁移下一个模块菜单。
