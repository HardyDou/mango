# 常见问题与排障

这里收录“能力已经接入，但运行结果不符合预期”的定位路径。它不是业务场景手册：先确认现象，再沿着请求、授权、租户和前端注册链路缩小范围。

## 快速选择

| 现象 | 入口 | 先确认 |
|---|---|---|
| 菜单可见但点击后空白、404 或组件加载失败 | [菜单页面打不开](./rbac-menu-page-troubleshooting.md) | 菜单 `component`、页面注册、资源加载和运行时请求 |
| 页面能打开，但按钮不显示或点击 403 | [按钮权限不显示](./permission-button-troubleshooting.md) | `permissionCode`、角色授权、登录态权限和接口鉴权 |
| 字典、配置、组织或基础数据为空 | [租户字典配置为空](./tenant-dict-config-empty.md) | `tenantId`、初始化边界、Resource 同步和数据过滤 |

## 通用排障顺序

1. **确认范围**：记录租户、用户、应用、版本、页面 URL 和完整错误码；先判断是单账号、单租户还是全局问题。
2. **确认请求**：在浏览器 Network 和服务端日志中找到实际请求，区分 401、403、404、5xx 和空成功响应。
3. **确认事实来源**：菜单/权限看服务端授权结果，页面看注册表，基础数据看目标表和 Resource/Bootstrap receipt；不要只凭 UI 推断。
4. **确认发布闭环**：核对后端版本、前端包矩阵、Resource generation/fingerprint 和数据库 migration 状态。
5. **最小复现与回归**：用一个测试租户和测试账号完成正向、反向及跨租户验证，保留可复核证据。

## 相关入口

- [Mango 能力地图](../../capabilities/README.md)
- [业务接入场景](../business-integration/README.md)
- [平台模块 README](../../capabilities/README.md)
- [PMO 能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [Issue Runbook](../../../mango-pmo/rules/07-mango-issue-runbook.md)

排障指南只记录当前定位步骤；版本影响、Issue 讨论和验证证据放在 [设计文档](../../designs/)、[交付计划](../../plans/)、[验收证据](../../evidence/) 或根 [CHANGELOG](../../../CHANGELOG.md)。
