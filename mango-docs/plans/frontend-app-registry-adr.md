# ADR：前端应用入口注册表

## 决策

将 `authorization_app` 从普通授权应用 CRUD 升级为逻辑应用注册入口。应用表示用户入口和授权边界；前端运行配置独立保存在 `frontend_app_registry`；后端 Maven 模块继续表示领域能力，不与前端应用一一绑定。

## 术语

- 应用：稳定的用户入口和授权边界，以 `appCode` 唯一标识，例如 `internal-admin`。
- 前端运行单元：应用在浏览器中加载和隔离的配置，包括类型、部署模式、入口地址、挂载路径和激活规则。
- 菜单：应用下的导航和权限节点，继续归属 `appCode`。
- 模块：后端领域能力或工程模块，不直接决定前端入口数量。
- 部署单元：实际打包和运行的产物，例如 `mango-admin`、`mango-admin-shell`、`mango-platform-app`。
- 微应用：远程加载的前端运行单元，是应用的一种部署形态，不等同于新的授权应用。

## 约束

- `internal-admin` 是默认内置本地应用，必须保持单体打包可用。
- 主框架只依赖统一协议和 adapter，不直接绑定 qiankun、wujie 等具体微前端引擎。
- 第一阶段只落地 local、iframe、link 的协议和 adapter；微前端引擎只预留扩展点。

## 管理后台示例

`internal-admin` 是一个逻辑应用，不因部署形态变化而拆成多个授权应用。

单体版管理后台：

```text
appCode=internal-admin
appType=LOCAL
deployMode=EMBEDDED
部署单元=mango-ui/apps/mango-admin
```

微前端版管理后台：

```text
appCode=internal-admin
appType=MICRO_APP
deployMode=HYBRID
主壳部署单元=mango-ui/apps/mango-admin-shell
子应用示例=mango-ui/apps/mango-admin-rbac-app
```

两种形态共享同一个授权边界、租户开通关系和菜单权限树；差异只体现在前端运行配置与菜单页面运行类型。
