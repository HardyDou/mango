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

## 3. 编码规则

- 模块必须在 `META-INF/mango/module.properties` 中声明稳定 `module-name`、`module-path` 和模块编号。
- 模块编号为 3 位数字，全平台唯一，并登记在公开模块清单中。
- 菜单编码必须长期稳定，优先使用业务语义编码；需要数字排序编号时使用 `模块编号(3位) + 子模块编号(3位) + 菜单编号(4位)`。
- 模块根菜单或一级能力目录的数字编号以模块编号开头。
- 子模块编号为 3 位，同一模块内唯一。
- 子模块内菜单编号为 4 位，同一子模块内唯一。
- 权限码不得和同一模块内菜单 `menuCode` 复用；按钮菜单需要复用接口权限时，必须显式提供独立 `menuCode` 和真实 `permissionCode`。
- `permissionItems.packageCodes`、`permissionItems.roleCodes` 未配置时继承所属菜单；显式空数组表示不加入套餐或不授权角色。

## 4. 前端一致性

- 菜单 `path` 必须匹配前端路由。
- 菜单 `component` 必须匹配前端页面注册键。
- 按钮权限码必须匹配前端真实权限点和后端接口访问策略。

## 5. 部署与同步

- 单体部署入口必须装配本地 Resource Registry runtime 和资源同步入口。
- 微服务或能力 app 只上报本服务资源声明时，必须通过远程 Resource Registry starter 上报到资源中心。
- Resource 服务接收远程声明后，必须能根据资源 `targetModule` 找到本地 handler 或远程 target dispatcher。
- 同一批资源存在多个 `targetModule` 时必须按目标模块分桶同步。
- 缺失资源禁用必须按来源应用、来源服务和模块边界计算，不得跨服务误禁用其它服务的资源。

## 6. 禁止事项

- 禁止把功能模块菜单初始化 SQL 写入非归属模块。
- 禁止用 Flyway SQL 维护菜单、按钮权限、菜单运行时配置、套餐授权和默认角色授权数据。
- 禁止用 README、设计文档或 evidence 代替资源清单。
- 禁止让资源清单覆盖或删除清单中不存在的历史授权资产。

## 7. 验收要求

- 菜单迁移必须按模块逐个处理。
- 每迁移一个模块，必须完成菜单树截图、菜单入口路径、关键页面打开结果、console/network 检查和用户人工确认。
- 未完成人工确认前，不得继续迁移下一个模块菜单。
- 发布前必须在干净库验证 Resource Registry 同步、`AUTH_MENU` handler 消费、菜单无孤儿、套餐绑定、角色授权、`/auth/info` 权限和当前用户菜单树。
