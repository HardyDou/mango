# @mango/calendar 使用说明

`@mango/calendar` 是 Mango 管理后台的工作日历前端包。它提供日历管理页面注册函数、工作日历 API 封装、日期类型选项和格式化工具。

本包属于 `admin-pages` 配套能力，不是官网、营销站、C 端页面或普通日期选择组件。业务页面如果只需要计算工作日，可以直接使用 `calendarApi` 或调用后端 HTTP 接口，不需要注册管理页。

## 1. 概览

`@mango/calendar` 对外提供：

- `registerMangoCalendarAdminPages()`：把日历管理页注册到 Mango Admin Pages。
- `CalendarView`：日历管理页面组件。
- `calendarApi`：日历定义、年度、日期、工作日计算、农历查询 API 封装。
- `dayTypeOptions`、`weekDayOptions`、`dayTypeLabel()`、`weekDayLabel()`：页面常用选项和显示工具。

后端依赖是 `mango-calendar`。菜单、权限和日历数据都由后端提供。

## 2. 功能清单

| 能力 | 用途 | 导出 |
|------|------|------|
| 管理页注册 | 在 Admin Shell 中打开日历管理页 | `registerMangoCalendarAdminPages` |
| 日历管理页面 | 日历定义、年度初始化、日期维护、工作日工具 | `CalendarView` |
| 日历定义 API | 查询、新增、编辑、启停、删除日历 | `calendarApi.pageCalendars` 等 |
| 年度 API | 初始化、查询、启停、删除年度 | `calendarApi.initCalendarYear` 等 |
| 日期 API | 查询、编辑、批量设置、导入日期 | `calendarApi.pageCalendarDays` 等 |
| 工作日 API | 判断、偏移、统计、月份汇总 | `calendarApi.isWorkday` 等 |
| 农历 API | 农历查询、农历转公历、节气查询 | `calendarApi.lunarDay` 等 |
| 选项工具 | 日期类型和星期显示 | `dayTypeOptions`、`weekDayOptions` |

## 3. 集成形态

| 标识 | 是否适用 | 说明 |
|------|----------|------|
| `admin-shell` | 否 | 本包不提供管理后台壳能力。 |
| `admin-pages` | 是 | `registerMangoCalendarAdminPages()` 注册 `data/calendar/index` 页面。 |
| `business-component` | 否 | 本包没有可复用业务组件。 |
| `api-client` | 是 | `calendarApi` 可在业务前端直接调用后端日历接口。 |

如果是官网或 C 端站点，不要集成 `CalendarView`；只按需要调用 `calendarApi` 或后端 HTTP。

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/calendar
```

注册管理页：

```ts
import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';

registerMangoCalendarAdminPages();
```

引入 API：

```ts
import { calendarApi, dayTypeOptions } from '@mango/calendar';
```

依赖包：

| 依赖 | 说明 |
|------|------|
| `@mango/admin-pages` | 管理页注册能力。 |
| `@mango/api-schema` | `ApiId` 类型。 |
| `@mango/common` | 请求工具。 |
| `element-plus` | 页面 UI peer dependency。 |
| `vue` | Vue 3 peer dependency。 |

## 5. 快速开始

### 5.1 注册日历管理页

```ts
import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';

export function registerAdminPages() {
  registerMangoCalendarAdminPages();
}
```

注册后页面 key 是：

```text
data/calendar/index
```

Admin Shell 需要从后端菜单拿到该页面 key，才能在左侧菜单打开页面。

### 5.2 业务页面计算工作日

```ts
import { calendarApi } from '@mango/calendar';

const isWorkday = await calendarApi.isWorkday({
  calendarCode: 'CN_STANDARD',
  date: '2026-06-16',
});

const dueDate = await calendarApi.addWorkdays({
  calendarCode: 'CN_STANDARD',
  sourceDate: '2026-06-16',
  amount: 5,
  includeSource: false,
});
```

### 5.3 查询月份汇总

```ts
const summary = await calendarApi.monthSummary({
  calendarCode: 'CN_STANDARD',
  year: 2026,
  month: 6,
});
```

## 6. 配置说明

本包没有独立运行时配置文件。页面是否能打开由以下后端数据决定：

| 配置来源 | 说明 |
|----------|------|
| 菜单 | 后端菜单的组件 key 必须是 `data/calendar/index`。 |
| 权限 | 页面按钮使用 `v-auth` 权限码控制。 |
| 后端接口 | API 请求路径以 `/calendar` 开头。 |
| 日历数据 | 日历下拉、年度和日期来自 `mango-calendar` 数据库。 |

## 7. API 与扩展

### 7.1 页面注册

| 导出 | 说明 |
|------|------|
| `registerMangoCalendarAdminPages()` | 注册 `mango-calendar` 模块页面。重复调用会自动跳过。 |
| `CalendarView` | 日历管理页面组件。 |

页面注册信息：

| 字段 | 值 |
|------|----|
| `moduleCode` | `mango-calendar` |
| 页面 key | `data/calendar/index` |

### 7.2 日历管理 API

| API | HTTP | 用途 |
|-----|------|------|
| `pageCalendars(params)` | `GET /calendar/admin/calendars/page` | 分页查询日历。 |
| `listCalendarOptions(params)` | `GET /calendar/admin/calendars/options` | 查询日历选项。 |
| `createCalendar(data)` | `POST /calendar/admin/calendars` | 新增日历。 |
| `updateCalendar(data)` | `PUT /calendar/admin/calendars` | 更新日历。 |
| `updateCalendarStatus(id, status)` | `PUT /calendar/admin/calendars/status` | 启停日历。 |
| `deleteCalendar(id)` | `DELETE /calendar/admin/calendars` | 删除日历。 |
| `pageCalendarYears(params)` | `GET /calendar/admin/years/page` | 分页查询年度。 |
| `initCalendarYear(data)` | `POST /calendar/admin/years/init` | 初始化年度。 |
| `refreshCalendarYearLunar(data)` | `PUT /calendar/admin/years/lunar` | 刷新年度农历信息。 |
| `updateCalendarYearEnabled(calendarCode, year, enabled)` | `PUT /calendar/admin/years/enabled` | 启停年度。 |
| `deleteCalendarYear(calendarCode, year)` | `DELETE /calendar/admin/years` | 删除年度。 |
| `yearSummary(calendarCode, year)` | `GET /calendar/admin/years/summary` | 查询年度汇总。 |
| `pageCalendarDays(params)` | `GET /calendar/admin/days/page` | 分页查询日期。 |
| `updateCalendarDay(data)` | `PUT /calendar/admin/days` | 更新日期。 |
| `deleteCalendarDay(id)` | `DELETE /calendar/admin/days` | 删除日期。 |
| `batchUpdateCalendarDays(data)` | `PUT /calendar/admin/days/batch` | 批量更新日期。 |
| `importCalendarDays(data)` | `POST /calendar/admin/days/import` | 导入日期。 |

### 7.3 工作日和农历 API

| API | HTTP | 用途 |
|-----|------|------|
| `getDay(params)` | `GET /calendar/workdays/day` | 查询日期属性。 |
| `isWorkday(params)` | `GET /calendar/workdays/check` | 判断是否工作日。 |
| `nextWorkday(params)` | `GET /calendar/workdays/next` | 查询下一个工作日。 |
| `previousWorkday(params)` | `GET /calendar/workdays/previous` | 查询上一个工作日。 |
| `addWorkdays(params)` | `GET /calendar/workdays/add` | 工作日偏移。 |
| `countWorkdays(params)` | `GET /calendar/workdays/count` | 区间工作日统计。 |
| `monthSummary(params)` | `GET /calendar/workdays/month/summary` | 月份工作日汇总。 |
| `nthWorkdayOfMonth(params)` | `GET /calendar/workdays/month/nth` | 查询月份第 N 个工作日。 |
| `lunarDay(params)` | `GET /calendar/lunar/day` | 查询农历信息。 |
| `lunarToSolar(params)` | `GET /calendar/lunar/to-solar` | 农历转公历。 |
| `solarTerms(params)` | `GET /calendar/lunar/solar-terms` | 查询年度节气。 |

常用类型：

| 类型 | 说明 |
|------|------|
| `CalendarVO` | 日历定义。 |
| `CalendarYearSummaryVO` | 年度汇总。 |
| `CalendarDayVO` | 日期明细。 |
| `MonthWorkdaySummaryVO` | 月份工作日汇总。 |
| `LunarDayInfoVO` | 农历日期信息。 |
| `SolarTermVO` | 节气信息。 |

## 8. 数据与初始化

本包不包含数据库 migration。它依赖后端完成：

| 数据 | 后端来源 | 前端表现 |
|------|----------|----------|
| 菜单 | authorization 迁移 | 左侧菜单打开 `data/calendar/index`。 |
| 权限 | authorization 迁移和角色授权 | 页面按钮按 `v-auth` 显示或隐藏。 |
| 日历定义 | `mango-calendar` | 日历列表和下拉。 |
| 年度日期 | `mango-calendar` | 年度列表、日期列表、工作日计算。 |
| 默认 `CN_STANDARD` | `mango-calendar` 迁移 | 可以直接查询 2025-2026 年。 |

页面使用的权限码：

```text
calendar:admin:list
calendar:admin:create
calendar:admin:edit
calendar:admin:status
calendar:admin:delete
calendar:year:list
calendar:year:init
calendar:year:enabled
calendar:year:delete
calendar:day:list
calendar:day:edit
calendar:day:batch
calendar:day:delete
calendar:calculate:query
```

## 9. 管理入口

| 入口 | 值 |
|------|----|
| 页面 key | `data/calendar/index` |
| 模块编码 | `mango-calendar` |
| 后端模块 | `mango/mango-platform/mango-calendar` |
| 前端页面文件 | `src/views/calendar/index.vue` |
| 页面注册文件 | `src/admin-pages.ts` |

## 10. 问题排查

| 现象 | 常见原因 | 处理 |
|------|----------|------|
| 菜单点击后空白 | 页面 key 未注册或菜单组件 key 不一致 | 确认调用 `registerMangoCalendarAdminPages()`，菜单 key 是 `data/calendar/index`。 |
| 页面按钮不显示 | 当前账号没有权限码 | 给角色授权对应 `calendar:*` 权限。 |
| 日历下拉为空 | 后端没有日历定义或日历停用 | 在日历管理中新建并启用日历。 |
| 初始化年度失败 | 年度已存在且 `overwrite=false` | 勾选覆盖或删除年度后重试。 |
| 工作日计算提示年度未初始化 | 传入日期所在年度没有日期明细 | 初始化对应年度并导入节假日。 |
| API 返回日期格式异常 | 后端日期序列化为数组 | `calendarApi` 已做 `normalizeDate()`，业务代码优先使用 `calendarApi`。 |

## 11. 相关文档

- [Mango Calendar 后端说明](../../../mango/mango-platform/mango-calendar/README.md)
- [Admin Pages 使用说明](../admin-pages/README.md)
- [Mango Common 使用说明](../common/README.md)
- [能力文档维护规则](../../../mango-pmo/rules/08-capability-docs.md)
