# 日历初始化内置中国法定节假日和调休交付契约

## 1. 目标

新库初始化时内置中国标准工作日历，包含 2025 年和 2026 年中国法定节假日、连休日期和调休上班日，避免管理员手工补录后才能使用工作日计算。

## 2. 范围

- `mango-calendar` 模块 Flyway 初始化数据。
- 默认租户 `tenant_id=1` 的 `CN_STANDARD` 日历。
- `calendar_day` 覆盖 2025-01-01 至 2026-12-31 每日数据。
- 节假日/调休来源按国务院办公厅 2025、2026 年部分节假日安排通知登记。

## 3. 不做什么

- 不修改已执行的历史 migration。
- 不新增或调整 API。
- 不改菜单、页面、权限。
- 不在 SQL 中计算农历字段，农历仍由现有日历农历能力维护。

## 4. 设计输入

- 用户要求：初始化数据中要内置中国法定节假日/调休。
- 2025 年国务院办公厅节假日安排通知。
- 2026 年国务院办公厅节假日安排通知。

## 5. 设计说明

### 5.1 影响模块

- `mango-platform/mango-calendar/mango-calendar-core`

### 5.2 接口变化

无。

### 5.3 数据变化

- 新增 `V4__default_china_calendar_2025_2026.sql`。
- 初始化 `calendar`：`CN_STANDARD` / `中国标准工作日历`。
- 初始化 `calendar_day`：2025、2026 全量日期。
- 默认周一至周五为 `WORKDAY`，周六周日为 `RESTDAY`。
- 法定节假日/连休日期覆盖为 `LEGAL_HOLIDAY`。
- 调休上班日覆盖为 `ADJUSTED_WORKDAY`。
- 对已有行，仅当 `source` 为空、`系统默认`、`国务院办公厅` 时覆盖，避免覆盖人工维护数据。

### 5.4 菜单/页面/权限变化

无。

### 5.5 测试范围

- 日历模块集成测试。
- `mango-check` persistence-schema 检查。
- 关键调休日期静态核对。

## 6. 风险与限制

- `mango-check` 对历史 `V1__init_calendar.sql` 会报当前审计字段规范问题；本次按规范不修改历史 migration。
- 新增初始化数据面向默认租户 `tenant_id=1`，其它租户仍需通过现有日历管理能力维护。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 初始化数据内置中国法定节假日 | 新增 calendar 模块 Flyway 数据迁移，内置 `CN_STANDARD` 日历及 2025/2026 全量日期 | `mango/mango-platform/mango-calendar/mango-calendar-core/src/main/resources/db/migration/calendar/V4__default_china_calendar_2025_2026.sql` | `mvn -f mango/pom.xml -pl mango-platform/mango-calendar/mango-calendar-core -am test` | DONE | calendar-core 集成测试通过 |
| TASK-002 | 用户要求 | 初始化数据内置调休上班日 | 调休日写入 `ADJUSTED_WORKDAY` 且 `workday=1` | `V4__default_china_calendar_2025_2026.sql` | `rg` 核对 2025/2026 关键调休日期 | DONE | SQL 包含 2025-01-26、2025-02-08、2025-04-27、2025-09-28、2025-10-11、2026-02-14、2026-02-28、2026-05-09、2026-09-20、2026-10-10 |
| TASK-003 | PMO 数据规范 | 数据变更使用新增 migration，不修改历史 migration | 新增 `V4`，不改 `V1/V2/V3` | `V4__default_china_calendar_2025_2026.sql` | `git diff --name-only` | DONE | 仅新增 V4 和本交付台账 |
