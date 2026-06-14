# 日历 Calendar

## 1. 能力定位

提供租户内业务日历、工作日和节假日查询能力。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务排班、任务调度、流程时限和编号生成需要按租户日历判断工作日时使用。

## 3. 不适用场景

不负责考勤排班业务规则、外部节假日源同步和前端日历组件展示。

## 4. 模块边界

包含 `mango-calendar-api`、`mango-calendar-core`、`mango-calendar-starter` 和 remote starter；依赖 persistence、context、kv 与 access。

## 5. 接入方式

后端引入 `mango-calendar-starter`；远程调用引入 `mango-calendar-starter-remote`。HTTP 入口包含 `/calendar` 和 `/calendar/admin`。

## 6. 配置项

`mango.calendar.kv` 用于日历日期缓存；模块开关跟随 starter 自动配置。

## 7. 对外接口 / 扩展点

`CalendarApi`、`CalendarAdminApi`、`CalendarFeignClient`；Controller 为 `CalendarController`、`CalendarAdminController`。

## 8. 数据库 / 初始化数据

`db/migration/calendar/V1__init_calendar.sql` 创建 `calendar`、`calendar_day` 并初始化默认日历数据。

## 9. 菜单 / 权限 / 租户

按租户隔离日历数据；管理入口使用当前租户上下文，权限资源由接口注解和菜单资源接入 authorization。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-calendar -am test
```

## 11. 业务接入最小闭环

业务模块引入 starter 后设置租户上下文，创建或复用租户日历，调用 `CalendarApi` 查询日期属性，并断言跨租户不可读取。

## 12. 常见问题

如果查询为空，先检查租户上下文、migration 是否执行、`mango.calendar.kv` 对应 KV 能力是否可用。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
