# 系统前端包

## 1. 能力定位

提供字典、参数、租户、地区、日志等系统页面、组件和 API，并承载系统事件运维前端页面。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务前端需要读取系统配置、字典或租户信息时使用；后台需要展示事件 outbox 运维页面时，可复用本包的 `SystemEventView`。

## 3. 不适用场景

不负责后端系统表初始化，也不提供事件后端能力。

## 4. 模块边界

包名：`@mango/system`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。`SystemEventView` 和 `eventApi` 是事件运维前端入口，后端 `/system/events` 归属 `mango-infra-event`。

## 5. 接入方式

安装并引入：

```ts
import '@mango/system/style.css';
import { AreaView, ConfigView, DictView, TenantView } from '@mango/system';
import { dictApi, tenantApi, configApi } from '@mango/system';
```

字典、参数、租户、地区和日志页面需要接入 [System](../../../mango/mango-platform/mango-system/README.md)。事件页面需要接入 [Event](../../../mango/mango-infra/mango-infra-event/README.md)，接口路径是 `/system/events`。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口：

- 页面：`DictView`、`TenantView`、`ConfigView`、`AreaView`、`OperationLogView`、`LoginLogView`、`SystemEventView`。
- 组件：`ParticipantSelector`、`DomainSelector`、`DomainSideTree`。
- API：`dictApi`、`tenantApi`、`configApi`、`areaApi`、`eventApi`。其中 `eventApi` 调用 `mango-infra-event` 暴露的 `/system/events`。

主要 API 前缀：`/system`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/system build
```

## 11. 业务接入最小闭环

业务页面读取字典、参数、租户和地区 API，确认数据来自后端；如使用事件页面，同时确认 `/system/events` 可访问。

## 12. 常见问题

字典为空优先检查 System migration 和字典类型；租户为空优先检查 seed 或租户初始化；事件为空优先检查 Event outbox 和 `/system/events` 条件装配。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
