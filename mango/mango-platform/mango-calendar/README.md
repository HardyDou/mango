# 日历 Calendar

## 1. 概览
`mango-calendar` 提供租户内工作日历能力：维护日历定义和年度日期，按日历编码判断工作日、做工作日偏移、统计区间工作日，并提供农历和二十四节气查询。

主要使用者是流程时限、任务调度、编号生成、合同履约、售后 SLA 等需要“按工作日计算”的业务模块。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务需要按租户维护不同工作日历，例如中国标准工作日历、企业自定义运营日历 | Maven 依赖 / HTTP API / Java API |
| 业务需要判断某天是否工作日、查询上一个或下一个工作日、计算 N 个工作日后的日期 | Maven 依赖 / HTTP API / Java API |
| 业务需要按月统计工作日、按区间列出日期属性，或者展示农历、生肖、干支、节气信息 | Maven 依赖 / HTTP API / Java API |
| 管理端需要初始化年度 365/366 天数据，并手动覆盖节假日、调休上班日、临时开放日 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务需要按租户维护不同工作日历，例如中国标准工作日历、企业自定义运营日历。
- 业务需要判断某天是否工作日、查询上一个或下一个工作日、计算 N 个工作日后的日期。
- 业务需要按月统计工作日、按区间列出日期属性，或者展示农历、生肖、干支、节气信息。
- 管理端需要初始化年度 365/366 天数据，并手动覆盖节假日、调休上班日、临时开放日。

## 4. 边界说明
- 不负责考勤、排班、请假、工时核算等人事业务规则。
- 不自动同步外部节假日源；国家法定节假日变化后，需要通过管理接口或迁移数据更新。
- 不提供前端日历组件展示，前端包只负责管理页面和调用接口。

## 5. 模块组成
- `mango-calendar-api`：`CalendarApi`、`CalendarAdminApi`、查询对象、命令对象和 VO。
- `mango-calendar-core`：`calendar`、`calendar_day` 实体、Mapper、工作日计算、年度初始化、农历换算、KV 缓存。
- `mango-calendar-starter`：注册 `CalendarAutoConfiguration`，暴露 `/calendar` 和 `/calendar/admin` HTTP 接口。
- `mango-calendar-starter-remote`：注册 `CalendarFeignClient`，供微服务远程调用。

调用方负责设置租户上下文、选择日历编码，并决定业务字段里保存哪一个日历编码。

## 6. 接入方式
单体或提供日历接口的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.calendar</groupId>
    <artifactId>mango-calendar-starter</artifactId>
</dependency>
```

只消费远程日历能力的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.calendar</groupId>
    <artifactId>mango-calendar-starter-remote</artifactId>
</dependency>
```

业务代码优先注入 `CalendarApi`；需要维护日历定义和年度日期时注入 `CalendarAdminApi` 或调用管理端接口。

## 7. 配置说明
配置前缀：`mango.calendar.kv`。

| 配置项 | 类型 | 默认值 | 含义 |
|--------|------|--------|------|
| `day-cache-ttl-seconds` | long | `86400` | 年度日期计算结果写入 KV 后的缓存秒数。适合工作日查询高频、年度数据低频变更的场景。 |

配置示例：

```yaml
mango:
  calendar:
    kv:
      day-cache-ttl-seconds: 86400
```

缓存依赖 `mango-infra-kv` 提供的 `IKvStore`。如果修改了某个年度的日期属性，管理服务会按实现逻辑刷新相关查询结果；验收时仍建议先查数据库，再查工作日接口确认结果一致。

## 8. API 与扩展
业务查询接口根路径：`/calendar`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/calendar/workdays/day` | 查询某天最终日期属性。 |
| GET | `/calendar/workdays/check` | 判断某天是否工作日。 |
| GET | `/calendar/workdays/next` | 查询指定日期后的下一个工作日。 |
| GET | `/calendar/workdays/previous` | 查询指定日期前的上一个工作日。 |
| GET | `/calendar/workdays/add` | 按工作日数量做日期偏移。 |
| GET | `/calendar/workdays/count` | 统计日期区间内工作日数量。 |
| GET | `/calendar/workdays/list` | 查询区间内每日属性。 |
| POST | `/calendar/workdays/batch-check` | 批量判断多个日期。 |
| GET | `/calendar/workdays/month/summary` | 查询月份工作日、休息日、第一个和最后一个工作日。 |
| GET | `/calendar/workdays/month/first` | 查询月份第一个工作日。 |
| GET | `/calendar/workdays/month/last` | 查询月份最后一个工作日。 |
| GET | `/calendar/workdays/month/nth` | 查询月份第 N 个工作日。 |
| GET | `/calendar/lunar/day` | 公历日期转农历信息。 |
| GET | `/calendar/lunar/to-solar` | 农历日期转公历日期。 |
| GET | `/calendar/lunar/solar-terms` | 查询年度二十四节气。 |

管理接口根路径：`/calendar/admin`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/calendar/admin/calendars/page` | 分页查询日历定义。 |
| GET | `/calendar/admin/calendars/options` | 查询可选日历。 |
| POST | `/calendar/admin/calendars` | 新增日历。 |
| PUT | `/calendar/admin/calendars` | 修改日历名称等基础信息。 |
| PUT | `/calendar/admin/calendars/status` | 启用或停用日历。 |
| DELETE | `/calendar/admin/calendars` | 删除日历及年度日期。 |
| GET | `/calendar/admin/years/page` | 分页查询已初始化年度。 |
| POST | `/calendar/admin/years/init` | 生成指定年度完整日历数据。 |
| PUT | `/calendar/admin/years/lunar` | 刷新年度农历、生肖、干支和节气字段。 |
| PUT | `/calendar/admin/years/enabled` | 启用或停用指定年度全部日期。 |
| DELETE | `/calendar/admin/years` | 删除指定日历年度。 |
| GET | `/calendar/admin/years/summary` | 查询年度汇总。 |
| GET | `/calendar/admin/days/page` | 分页查询年度日期明细。 |
| PUT | `/calendar/admin/days` | 更新单个日期工作日属性。 |
| PUT | `/calendar/admin/days/batch` | 批量更新日期属性。 |
| POST | `/calendar/admin/days/import` | 按日期覆盖导入工作日属性。 |
| DELETE | `/calendar/admin/days` | 删除单个日期明细。 |

## 9. 数据与初始化
Flyway 路径：`mango-calendar-core/src/main/resources/db/migration/calendar`。

`V1__init_calendar.sql` 创建并维护：

| 表 | 用途 |
|----|------|
| `calendar` | 租户内日历定义，`tenant_id + calendar_code` 唯一。 |
| `calendar_day` | 日历年度日期明细，包含公历日期、星期、日期类型、是否工作日、农历字段、来源和启用状态。 |

初始化数据：

- 默认租户：`tenant_id = 1`。
- 默认日历编码：`CN_STANDARD`。
- 默认日历名称：中国标准工作日历。
- 默认覆盖年份：2025 和 2026。
- 默认来源：国务院办公厅节假日安排和周末双休规则。

`calendar_day.day_type` 支持工作日、休息日、法定节假日、调休上班、临时开放、自定义开放等类型；是否工作日最终以 `workday` 字段和启用状态为准。

## 10. 管理入口
`calendar` 和 `calendar_day` 都带 `tenant_id`，查询和管理都应在当前租户上下文内执行。默认初始化数据只给 `tenant_id = 1`，新租户如果需要独立日历，要通过管理接口创建日历并初始化年度。

本模块后端接口当前没有在 Controller 上声明 `@ApiAccess` 权限码，接入管理菜单时要在前端菜单和 authorization 资源里显式绑定对应页面入口；不要假设引入 starter 后自动出现菜单。

## 11. 快速开始
1. 引入 `mango-calendar-starter` 或远程 starter。
2. 确认当前租户有可用日历编码，例如 `CN_STANDARD` 或业务自建编码。
3. 在业务配置中保存日历编码，不要把日期计算逻辑硬编码成周末双休。
4. 业务需要计算期限时调用 `CalendarApi.addWorkdays`、`countWorkdays` 或 `isWorkday`。
5. 管理员调整节假日后，业务重新调用接口获得最新结果。

## 12. 问题排查
- 查询为空：先确认租户上下文、日历编码、年度是否已初始化。
- 国家节假日变化：通过导入或批量更新日期接口维护，不会自动联网同步。
- 结果和数据库不一致：检查 `mango.calendar.kv.day-cache-ttl-seconds`、KV 后端是否正常，以及更新日期后是否经过管理服务刷新。
- 新租户无默认数据：默认 SQL 只初始化租户 1，新租户需要单独创建并初始化。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
