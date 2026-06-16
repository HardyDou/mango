# @mango/calendar

## 1. 概览
`@mango/calendar` 提供日历管理前端页面、工作日查询 API、农历转换 API 和页面注册函数。

本包属于 `admin-pages` 配套能力，依赖后端 `mango-calendar`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理工作日、休息日、法定节假日、调休补班、临时停工日和临时营业日 | 前端注册 / 组件 / API 封装 |
| 初始化或刷新某一年日历 | 前端注册 / 组件 / API 封装 |
| 业务系统需要查询下一工作日、上一工作日、加减工作日、统计区间工作日 | 前端注册 / 组件 / API 封装 |
| 页面包需要把日历管理页注册到 Mango 管理后台 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理工作日、休息日、法定节假日、调休补班、临时停工日和临时营业日。
- 初始化或刷新某一年日历。
- 业务系统需要查询下一工作日、上一工作日、加减工作日、统计区间工作日。
- 页面包需要把日历管理页注册到 Mango 管理后台。

## 4. 边界说明
- 不负责后端日历计算和节假日数据来源。
- 不负责业务单据截止日、SLA、排班等领域规则。
- 不初始化菜单和权限数据。
- 不适合作为普通日历展示组件直接用于官网。

## 5. 模块组成
本包包含：

- `CalendarView`：日历管理页面。
- `registerMangoCalendarAdminPages`：页面注册函数。
- `calendarApi`：日历、年份、日期、工作日、农历接口封装。
- `dayTypeOptions`：日期类型选项。

后端负责日历表、工作日算法、农历转换和权限校验。

## 6. 接入方式
安装：

```bash
pnpm add @mango/calendar
```

注册管理页面：

```ts
import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';

registerMangoCalendarAdminPages();
```

使用 API：

```ts
import { calendarApi } from '@mango/calendar';

const next = await calendarApi.nextWorkday({
  calendarCode: 'DEFAULT',
  date: '2026-06-15',
});
```

菜单 component key：

```text
data/calendar/index
```

## 7. 配置说明
本包没有运行时配置文件。行为由 API 参数和后端日历数据决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `registerMangoCalendarAdminPages` | `moduleCode` | `mango-calendar` | 页面归属模块 | 和后端菜单匹配 | `admin-pages.ts` |
| 页面注册 | `component` | `data/calendar/index` | 日历管理页 key | 菜单打开页面 | `admin-pages.ts` |
| `CalendarPageQuery` | `pageNum`、`pageSize` | 后端默认 | 日历分页 | 列表页分页 | `api/calendar.ts` |
| `CalendarPageQuery` | `keyword`、`status` | 空 | 日历过滤 | 列表查询 | `api/calendar.ts` |
| `InitCalendarYearCommand` | `calendarCode`、`year` | 无 | 初始化日历年份 | 生成年份日期 | `api/calendar.ts` |
| `InitCalendarYearCommand` | `overwrite`、`sourceYear` | 可选 | 覆盖或复制来源年 | 影响初始化策略 | `api/calendar.ts` |
| `UpdateCalendarDayCommand` | `dayType` | 无 | 日期类型 | 影响是否工作日 | `api/calendar.ts` |
| `AddWorkdaysQuery` | `includeSource` | 可选 | 是否包含起始日期 | 影响加减工作日结果 | `api/calendar.ts` |
| `CountWorkdaysQuery` | `includeStart`、`includeEnd` | 可选 | 是否包含边界 | 影响统计结果 | `api/calendar.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `CalendarView` | 日历管理页 |
| `registerMangoCalendarAdminPages` | 注册管理页 |
| `calendarApi.pageCalendars` | 日历列表 |
| `calendarApi.createCalendar`、`updateCalendar`、`deleteCalendar` | 日历 CRUD |
| `calendarApi.initCalendarYear`、`refreshCalendarYearLunar` | 年份初始化和农历刷新 |
| `calendarApi.pageCalendarDays`、`updateCalendarDay`、`batchUpdateCalendarDays` | 日期管理 |
| `calendarApi.importCalendarDays` | 批量导入日期 |
| `calendarApi.isWorkday`、`nextWorkday`、`previousWorkday` | 工作日判断 |
| `calendarApi.addWorkdays`、`countWorkdays` | 工作日计算 |
| `calendarApi.lunarDay`、`lunarToSolar`、`solarTerms` | 农历和节气查询 |
| `dayTypeOptions` | 日期类型选项 |

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端 `mango-calendar` 初始化表、权限和基础日历数据。

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 日历定义 | `mango-calendar` | 日历列表和选项 | `pageCalendars` 有数据 |
| 年份日期 | `mango-calendar` | 年份、日期管理和工作日计算 | 初始化年份后日期列表有数据 |
| 农历数据 | `mango-calendar` | 农历、节气展示 | `lunarDay`、`solarTerms` 可用 |
| 菜单权限 | authorization / calendar resource | 页面入口和按钮权限 | 菜单可见、接口可用 |

## 10. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 日历管理 | `data/calendar/index` | 后端 calendar 模块定义 | 后端 resource / migration | 角色授权 | calendar admin API |

租户边界以后端 calendar 数据模型为准；前端请求会通过 `@mango/common` 带当前租户头。

## 11. 快速开始
1. 后端启用 `mango-calendar`。
2. 前端安装并注册 `@mango/calendar/admin-pages`。
3. 后端初始化菜单权限并给角色授权。
4. 创建日历并初始化年份。
5. 在业务页面调用工作日 API。
6. 做页面构建、菜单验收和工作日计算验收。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 日历下拉为空 | 后端没有日历定义 | 创建日历或初始化数据 |
| 工作日计算不符合预期 | 年份未初始化或日期类型未调整 | 查年份日期列表 |
| 页面打不开 | component key 没注册 | 调用 `registerMangoCalendarAdminPages` |
| 接口 403 | 菜单可见但权限未授权 | 查角色权限和后端接口权限 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Calendar](../../../mango/mango-platform/mango-calendar/README.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
