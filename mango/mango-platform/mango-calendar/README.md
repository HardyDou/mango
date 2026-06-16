# Mango Calendar 使用说明

`mango-calendar` 是 Mango 的工作日历能力。业务系统用它统一维护日历、年度日期、工作日属性、节假日、调休补班、临时停工日、临时营业日，并按日历编码计算工作日。

业务模块不要自己硬编码周末、节假日和调休规则。需要算账期、工单 SLA、付款到期日、审批截止日时，保存业务自己的日期字段，工作日判断和偏移交给 `mango-calendar`。

## 1. 概览

`mango-calendar` 对外提供三类能力：

- 日历管理：创建日历、启停日历、初始化年度、维护年度日期。
- 工作日计算：判断某天是否工作日、计算前后工作日、统计区间工作日、查询月份工作日汇总。
- 农历与节气：按公历查农历、农历转公历、查询年度二十四节气。

默认日历编码是 `CN_STANDARD`，迁移脚本会初始化 2025-2026 年中国标准工作日历。其他年度需要在管理端初始化，或调用管理接口初始化。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 日历定义 | 维护不同国家、组织或业务线的日历编码 | `GET /calendar/admin/calendars/page`、`POST /calendar/admin/calendars`、`PUT /calendar/admin/calendars`、`DELETE /calendar/admin/calendars` |
| 日历选项 | 给业务表单提供日历下拉 | `GET /calendar/admin/calendars/options` |
| 年度初始化 | 生成某日历某一年的 365/366 天明细 | `POST /calendar/admin/years/init` |
| 年度启停 | 临时停用某一年日历数据 | `PUT /calendar/admin/years/enabled` |
| 日期维护 | 修改单日或批量日期的工作日属性 | `PUT /calendar/admin/days`、`PUT /calendar/admin/days/batch` |
| 日期导入 | 按日期覆盖导入节假日和调休数据 | `POST /calendar/admin/days/import` |
| 工作日判断 | 判断指定日期是否工作日 | `GET /calendar/workdays/check` |
| 工作日偏移 | 从某天开始向前或向后偏移 N 个工作日 | `GET /calendar/workdays/add` |
| 区间统计 | 统计两个日期之间的工作日数量 | `GET /calendar/workdays/count` |
| 月份汇总 | 查询某月工作日、休息日、第一个和最后一个工作日 | `GET /calendar/workdays/month/summary` |
| 农历查询 | 查询农历、生肖、干支纪年和节气 | `GET /calendar/lunar/day` |
| 节气查询 | 查询某年的二十四节气日期 | `GET /calendar/lunar/solar-terms` |

## 3. 后端接入

### 3.1 开发依赖

业务后端只需要调用 Java API 时，引入 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.calendar</groupId>
    <artifactId>mango-calendar-api</artifactId>
</dependency>
```

常用 Java API：

| API | 用途 |
|-----|------|
| `CalendarApi.getDay(CalendarDateQuery)` | 查询某天的日历属性。 |
| `CalendarApi.isWorkday(CalendarDateQuery)` | 判断某天是否工作日。 |
| `CalendarApi.nextWorkday(CalendarDateQuery)` | 查询下一个工作日。 |
| `CalendarApi.previousWorkday(CalendarDateQuery)` | 查询上一个工作日。 |
| `CalendarApi.addWorkdays(AddWorkdaysQuery)` | 按工作日偏移日期，`amount` 可为负数。 |
| `CalendarApi.countWorkdays(CountWorkdaysQuery)` | 统计区间工作日数量。 |
| `CalendarApi.monthSummary(MonthQuery)` | 查询月份工作日汇总。 |
| `CalendarApi.lunarDay(SolarDateQuery)` | 按公历日期查询农历信息。 |
| `CalendarApi.lunarToSolar(LunarDateQuery)` | 农历转公历。 |

计算付款到期日：

```java
AddWorkdaysQuery query = new AddWorkdaysQuery();
query.setCalendarCode("CN_STANDARD");
query.setSourceDate(LocalDate.parse("2026-06-16"));
query.setAmount(5);
query.setIncludeSource(false);

LocalDate dueDate = calendarApi.addWorkdays(query).getData();
```

判断 SLA 截止日是否工作日：

```java
CalendarDateQuery query = new CalendarDateQuery();
query.setCalendarCode("CN_STANDARD");
query.setDate(deadline);

boolean workday = calendarApi.isWorkday(query).getData();
```

### 3.2 部署依赖

提供日历服务的应用引入 `mango-calendar-starter`：

```xml
<dependency>
    <groupId>io.mango.platform.calendar</groupId>
    <artifactId>mango-calendar-starter</artifactId>
</dependency>
```

远程调用独立日历服务的应用引入 `mango-calendar-starter-remote`：

```xml
<dependency>
    <groupId>io.mango.platform.calendar</groupId>
    <artifactId>mango-calendar-starter-remote</artifactId>
</dependency>
```

`mango-calendar-starter-remote` 提供 `CalendarFeignClient`，服务名 `mango-calendar`，路径 `/calendar`。远程 starter 只覆盖工作日计算和农历查询接口；日历定义、年度初始化、日期维护是日历服务侧管理能力。

## 4. 前端接入

前端包是 `@mango/calendar`。它属于 `admin-pages` 配套插件，提供管理后台页面和 API 封装，不是官网、营销站或 C 端普通日历组件。

注册日历管理页：

```ts
import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';

registerMangoCalendarAdminPages();
```

页面 key：

| 管理页 | 页面 key | 用途 |
|--------|----------|------|
| 日历管理 | `data/calendar/index` | 日历定义、年度初始化、日期维护、工作日计算工具。 |

业务前端需要调用工作日接口时，可以使用 `calendarApi`：

```ts
import { calendarApi } from '@mango/calendar';

const nextDate = await calendarApi.addWorkdays({
  calendarCode: 'CN_STANDARD',
  sourceDate: '2026-06-16',
  amount: 5,
  includeSource: false,
});
```

如果项目不是 Admin Shell / Admin Pages 应用，也可以直接按 HTTP 接口调用后端，不需要注册 `@mango/calendar/admin-pages`。

## 5. 快速开始

### 5.1 用默认中国标准工作日历

1. 服务端启用 `mango-calendar-starter`。
2. Flyway 启用 `calendar` 模块迁移。
3. 前端管理端注册 `registerMangoCalendarAdminPages()`。
4. 给角色授权 `calendar:admin:list`、`calendar:year:list`、`calendar:day:list` 等权限。
5. 业务后端或前端按 `calendarCode=CN_STANDARD` 调用工作日接口。

### 5.2 初始化新年度

```http
POST /calendar/admin/years/init
Content-Type: application/json

{
  "calendarCode": "CN_STANDARD",
  "year": 2027,
  "overwrite": false
}
```

`sourceYear` 为空时按周一至周五工作日、周六周日休息日生成。需要复制某一年规则时传 `sourceYear`。

### 5.3 导入法定节假日和调休

```http
POST /calendar/admin/days/import
Content-Type: application/json

{
  "calendarCode": "CN_STANDARD",
  "year": 2027,
  "items": [
    {
      "date": "2027-01-01",
      "dayType": "LEGAL_HOLIDAY",
      "dayName": "元旦",
      "source": "国务院办公厅",
      "remark": "节假日安排"
    },
    {
      "date": "2027-01-03",
      "dayType": "ADJUSTED_WORKDAY",
      "dayName": "调休补班"
    }
  ]
}
```

## 6. 配置说明

`mango-calendar` 的行为主要由数据库中的日历和日期明细决定。YAML 只控制模块启用、远程调用启用和日历日结果缓存。

```yaml
mango:
  calendar:
    enabled: true
    kv:
      day-cache-ttl-seconds: 86400
    remote:
      enabled: true
  persistence:
    flyway:
      modules:
        calendar:
          enabled: true
```

## 7. YAML 配置字段

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.calendar.enabled` | `true` | 是否启用本地日历服务。关闭后不注册日历 controller、service 和 mapper。 |
| `mango.calendar.remote.enabled` | `true` | 是否启用 `CalendarFeignClient`。只在引入 `mango-calendar-starter-remote` 的应用生效。 |
| `mango.calendar.kv.day-cache-ttl-seconds` | `86400` | 日期查询结果缓存 TTL，单位秒。 |
| `mango.persistence.flyway.modules.calendar.enabled` | 无全局默认 | 是否执行 calendar 模块迁移。应用需要显式启用模块迁移。 |

## 8. 运行时配置字段

日历运行时配置保存在数据库，不走 YAML：

| 字段 | 来源 | 说明 |
|------|------|------|
| `calendarCode` | 日历定义 | 日历编码。业务调用工作日接口时必须传。 |
| `calendarName` | 日历定义 | 日历名称。 |
| `status` | 日历定义 | `1` 启用，`0` 停用。停用日历不能作为计算日历使用。 |
| `year` | 年度日期 | 年度，支持 1900-2100。 |
| `enabled` | 年度日期 | `1` 启用，`0` 停用。停用年度不能用于工作日计算。 |
| `dayType` | 日期明细 | 日期类型，决定 `workday`。 |
| `dayName` | 日期明细 | 节假日、调休或临时日期名称。 |
| `source` | 日期明细 | 数据来源，例如手工维护、国务院公告、导入。 |
| `remark` | 日期明细 | 备注。 |

日期类型：

| 类型 | 是否工作日 | 说明 |
|------|------------|------|
| `WORKDAY` | 是 | 普通工作日。 |
| `RESTDAY` | 否 | 普通休息日。 |
| `LEGAL_HOLIDAY` | 否 | 法定节假日。 |
| `ADJUSTED_WORKDAY` | 是 | 调休补班。 |
| `TEMP_CLOSED_DAY` | 否 | 临时停工、临时闭店等非工作日。 |
| `TEMP_OPEN_DAY` | 是 | 临时营业、临时上班等工作日。 |

`HOLIDAY`、`CUSTOM_CLOSED`、`CUSTOM_OPEN` 是兼容历史值，新数据不要继续使用。

## 9. 返回字段

`CalendarDayVO` 常用字段：

| 字段 | 说明 |
|------|------|
| `id` | 日期明细 ID。 |
| `calendarCode` | 日历编码。 |
| `calendarName` | 日历名称。 |
| `calendarYear` | 年度。 |
| `date` | 公历日期。 |
| `dayOfWeek` | 星期，`1` 周一，`7` 周日。 |
| `dayType` | 日期类型。 |
| `workday` | 是否工作日。 |
| `dayName` | 日期名称。 |
| `lunarYear`、`lunarMonth`、`lunarDay` | 农历年月日。 |
| `lunarLeapMonth` | 是否农历闰月。 |
| `lunarText` | 农历中文日期。 |
| `ganzhiYear` | 干支纪年。 |
| `zodiac` | 生肖。 |
| `solarTerm` | 节气。 |
| `source` | 数据来源。 |
| `remark` | 备注。 |
| `enabled` | 启用状态。 |

`CalendarYearSummaryVO` 返回年度总天数、工作日、休息日、法定假日、调休工作日、临时停工日、临时营业日和启用状态。

`MonthWorkdaySummaryVO` 返回月份总天数、工作日、休息日、第一个工作日和最后一个工作日。

## 10. 管理入口

| 入口 | 值 |
|------|----|
| 前端页面 key | `data/calendar/index` |
| 后端管理路径 | `/calendar/admin/**` |
| 工作日计算路径 | `/calendar/workdays/**` |
| 农历查询路径 | `/calendar/lunar/**` |
| 模块编码 | `mango-calendar` |

权限码：

| 权限 | 用途 |
|------|------|
| `calendar:admin:list` | 查询日历。 |
| `calendar:admin:create` | 新增日历。 |
| `calendar:admin:edit` | 编辑日历。 |
| `calendar:admin:status` | 启停日历。 |
| `calendar:admin:delete` | 删除日历。 |
| `calendar:year:list` | 查询年度。 |
| `calendar:year:init` | 初始化年度。 |
| `calendar:year:enabled` | 启停年度。 |
| `calendar:year:delete` | 删除年度。 |
| `calendar:day:list` | 查询日期。 |
| `calendar:day:edit` | 编辑日期。 |
| `calendar:day:batch` | 批量设置日期。 |
| `calendar:day:import` | 导入日期。 |
| `calendar:day:delete` | 删除日期。 |
| `calendar:calculate:query` | 使用页面上的工作日计算工具。 |

## 11. 数据与初始化

`mango-calendar` 自己初始化业务表和默认日历数据：

| 数据 | 来源 | 说明 |
|------|------|------|
| `calendar` | `db/migration/calendar/V1__init_calendar.sql` | 日历定义表。 |
| `calendar_day` | `db/migration/calendar/V1__init_calendar.sql` | 年度日期明细表。 |
| `CN_STANDARD` | `db/migration/calendar/V1__init_calendar.sql` | 默认中国标准工作日历。 |
| 2025-2026 年日期 | `db/migration/calendar/V1__init_calendar.sql` | 默认年度日期、法定节假日和调休数据。 |

菜单、应用模块和权限不在 calendar 模块内初始化，而是在 authorization 基线迁移中登记：

```text
mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V1__init_authorization.sql
```

如果页面看不到日历菜单，先确认 authorization 迁移已执行，再确认角色已授权对应权限码。

## 12. 问题排查

| 现象 | 常见原因 | 处理 |
|------|----------|------|
| 工作日接口提示日历不存在 | `calendarCode` 不存在或日历已停用 | 在日历管理中创建并启用日历。 |
| 提示年度日历未初始化 | 传入日期所在年度没有日期明细 | 初始化对应年度。 |
| 工作日结果不符合节假日安排 | 年度只按周末规则生成，未导入调休和法定假日 | 导入日期或手工批量设置。 |
| 管理页没有菜单 | authorization 菜单权限未初始化或角色未授权 | 检查 authorization 迁移和角色权限。 |
| 页面日历下拉为空 | 当前租户没有启用日历 | 创建日历或启用已有日历。 |
| 远程调用失败 | 没有启用 `mango-calendar-starter-remote` 或服务名不可达 | 检查远程 starter、服务发现和 `mango.calendar.remote.enabled`。 |

## 13. 相关文档

- [@mango/calendar 前端包](../../../mango-ui/packages/calendar/README.md)
- [Authorization 使用说明](../mango-authorization/README.md)
- [能力文档维护规则](../../../mango-pmo/rules/08-capability-docs.md)
