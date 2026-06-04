# Enterprise CLI 1.0.16 Runtime Acceptance

## Objective

Verify that an enterprise project generated from the published `@mango/cli@1.0.16` can run with real backend, frontend, database, Mango platform capabilities, and a generated business module.

Generated project:

- Path: `/tmp/mango-enterprise-cli-flow-116/contract-ops-platform`
- Preset: `full`
- Business module: `procurement-order`
- Aggregate: `procurement`
- Display name: `采购订单`

## Runtime

- Backend: `http://127.0.0.1:19055`
- Frontend: `http://127.0.0.1:19056`
- Database: `contract_ops_platform_runtime_116`
- Account: `admin / admin123`

Health check passed:

`curl -sS http://127.0.0.1:19055/actuator/health`

Result: `status=UP`, `db.status=UP`.

## Commands

Created an isolated database:

`mysql -h127.0.0.1 -P3306 -uroot -e "CREATE DATABASE IF NOT EXISTS contract_ops_platform_runtime_116 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"`

The generated README command `mvn -f backend/app/pom.xml spring-boot:run` failed on first run because the app module could not resolve the locally generated `procurement-order-starter` SNAPSHOT outside the backend reactor. To continue runtime verification, installed the generated backend reactor first:

`mvn -f backend/pom.xml -DskipTests install`

Started backend:

`MANGO_BACKEND_PORT=19055 MANGO_DB_URL='jdbc:mysql://127.0.0.1:3306/contract_ops_platform_runtime_116?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' MANGO_DB_USERNAME=root MANGO_DB_PASSWORD= MANGO_OFFICE_PLUGIN_ENABLED=false mvn -f backend/app/pom.xml spring-boot:run`

Started frontend:

`VITE_ADMIN_PROXY_PATH=http://127.0.0.1:19055 npm --prefix /tmp/mango-enterprise-cli-flow-116/contract-ops-platform/frontend run dev -- --host 127.0.0.1 --port 19056`

## Browser Verification

### Login and Shell

Status: passed.

- Login page rendered with `Mango Admin`, `芒果集团`, username/password inputs, and login button.
- Login with `admin/admin123` entered `#/home`.
- Home page displayed shell top navigation, left navigation, home content cards, user `admin`, tenant/org text `芒果集团`, and the notification bell.
- UI screenshot review: no obvious overlap or broken alignment on the checked viewport.

Evidence:

- `screenshots/01-login.png`
- `screenshots/02-home-after-login.png`

### Generated Business Module

Status: partially failed.

Failed:

- Clicking top-level business entry `采购订单` navigated to `#/procurement-order`.
- Page displayed 404.
- Root cause from read-only checks: generated parent menu is a directory (`menuType=1`) with `path=/procurement-order`, no component, and no redirect.

Passed:

- Clicking child menu `Procurement管理` navigated to `#/procurement-order/procurements`.
- Business page loaded with query input, query button, create button, and table.
- Created `验收采购订单-116`.
- The record appeared on the page with backend-generated id `2062058143104925697`.
- Database confirmed the record in `procurement_order_procurement`.

Evidence:

- `screenshots/03-procurement-order-404.png`
- `screenshots/04-procurement-list-empty.png`
- `screenshots/05-procurement-created.png`

Registered issue:

- https://github.com/HardyDou/mango/issues/66

### System Management

Status: passed.

- `系统管理` opened `#/system/menu-package`.
- Package list displayed real rows.
- Keyword query `平台` filtered the table to `平台管理套餐`.
- Reset restored the full list.
- `菜单管理` opened `#/system/menu`.
- Menu table showed generated business menu rows and confirmed the parent directory/child menu structure.
- UI screenshot review: table and actions were readable on the checked viewport; no obvious fixed-column overlap was observed in this page.

Evidence:

- `screenshots/06-system-menu-package.png`
- `screenshots/07-system-menu-with-procurement-config.png`

### Platform Capability

Status: passed.

- `平台能力` opened `#/data/calendar`.
- Calendar list displayed `中国标准工作日历`.
- `查看日期` opened the date detail list for 2026.
- Date detail displayed concrete rows and `Total 365`.
- UI screenshot review: content was readable; no obvious layout break on the checked viewport.

Evidence:

- `screenshots/08-calendar-list.png`
- `screenshots/09-calendar-day-detail.png`

### Workflow

Status: passed for list and query.

- `审批中心` opened `#/workflow/start-process`.
- Published process cards displayed `合同用印审批`, `费用报销审批`, and `请假申请`.
- Keyword query `请假` filtered the result to `请假申请`.
- Did not start a process instance in this pass.

Evidence:

- `screenshots/10-workflow-start-process.png`
- `screenshots/11-workflow-filtered.png`

### Tags View

Status: passed in this runtime.

- With multiple tabs open, closing active tab `发起流程` switched to the previous tab `日历管理`.
- The active route and page content both updated to `#/data/calendar`.

Evidence:

- `screenshots/12-tag-close-fallback-calendar.png`

### Notice Bell

Status: passed.

- Notification bell exists in DOM as `.notice-bell`.
- Trigger has `aria-label=消息提醒`.
- Clicking the bell opened the message popover.
- Popover displayed `我的消息`, `暂无消息`, `查看全部`, and `接收设置`.

Evidence:

- `screenshots/13-notice-bell-popover.png`

## Console and Network

Console:

- Errors: 0
- Warnings: 1
- Warning: WebSocket probe closed before established.
- SSE probe succeeded and runtime continued normally.

Network:

- Login, menu, notice unread count, business CRUD, menu package query, menu list, calendar list/detail, and workflow query requests returned 200.
- No failed HTTP request was observed in the final network log.

## Issues Registered

- #66: 企业 CLI 生成业务父目录点击进入 404
- #67: 企业 CLI 生成项目后端首次启动命令缺少本地业务模块构建前置

## Result

The generated enterprise project can run with real backend, frontend, database, Mango shell, system management, platform calendar, workflow list/query, notice bell, tag fallback, and generated business child page CRUD create/query.

It is not fully acceptable for enterprise promotion until #66 is fixed, because the generated top-level business directory opens 404 on first click. #67 should also be fixed to make first startup reliable for business developers.
