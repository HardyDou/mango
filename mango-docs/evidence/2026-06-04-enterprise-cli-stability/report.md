# Enterprise CLI Stability Verification

Date: 2026-06-04

## Scope

- Verified local `@mango/cli` source generation flow.
- Generated enterprise project: `finance-ops-admin`.
- Generated business module: `purchase/order`, display names `采购管理 / 采购订单`.
- Verified generated CRUD structure, backend build, frontend typecheck/build, real backend/frontend startup, browser CRUD flow.

## Generated CRUD Structure

The generated module includes:

- Backend API: `PurchaseApi`, `CreateOrderCommand`, `UpdateOrderCommand`, `OrderPageQuery`, `OrderVO`.
- Backend core: `OrderEntity`, `OrderMapper`, `IOrderService`, `OrderService`.
- Backend starter: `PurchaseController`, auto-configuration, resource manifest.
- Backend migration: `purchase_order` table.
- Frontend API package: `createOrder`, `updateOrder`, `deleteOrder`, `pageOrder`, `getOrderDetail`.
- Frontend page package: `purchase/order/index.vue`.

Static checks confirmed:

- `OrderEntity extends TenantEntity`.
- `OrderMapper extends BaseMapper<OrderEntity>`.
- `OrderService extends MangoCrudServiceImpl<OrderMapper, OrderEntity>`.
- `PurchaseController extends BaseCrudController`.
- Migration includes `id`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`.
- No generated mock/fake fixed CRUD response was found.

## Runtime Verification

Services:

- Backend: `http://127.0.0.1:19060`
- Frontend: `http://127.0.0.1:19061`
- Database: `mango_enterprise_cli_0604`

Browser validation:

- Login page rendered with tenant selector.
- Logged in as `admin/admin123`, tenant `芒果集团`.
- Home page rendered with `采购管理`, `系统管理`, `审批中心`, `平台能力`, `通知中心`.
- CRUD page `/purchase/orders` rendered.
- Completed create, query, detail, edit, delete through real UI.
- Final CRUD page showed `No Data`, `Total 0` for the queried record.

Screenshots:

- `01-login-page.png`
- `02-after-login-home.png`
- `03-purchase-list-empty.png`
- `04-purchase-create-dialog.png`
- `05-purchase-created.png`
- `06-purchase-query-result.png`
- `07-purchase-detail.png`
- `11-purchase-full-created-query.png`
- `12-purchase-full-detail.png`
- `13-purchase-full-edited.png`
- `14-purchase-full-delete-confirm.png`
- `15-purchase-full-after-delete.png`

UI review:

- Login/home/CRUD pages had no obvious 404, blank page, menu loss, top bar loss, tag view breakage, or visible layout overlap.
- CRUD table, toolbar, query form, dialog, drawer, operation column, and pagination were visible and aligned.
- The final CRUD page correctly showed empty data after deletion.

Console/network:

- No blocking console errors during login and CRUD.
- One realtime probe WebSocket/SSE abort appeared when the browser session was closing; no CRUD API failure was observed.

## Build Verification

Commands passed:

- `mvn -f backend/pom.xml -DskipTests install`
- `npm --prefix frontend install`
- `npm --prefix frontend run typecheck`
- `npm --prefix frontend run build`

Backend startup:

- `/actuator/health` returned `UP`.
- Flyway applied `purchase` migration.
- Resource manifest sync registered `purchase` menus and permissions.

Database checks:

- `purchase_order` table exists.
- Required tenant/audit columns exist.
- CRUD test records were cleaned up after validation.

## Issues Registered

- #90 `企业 CRUD 生成模块审计字段未自动填充`
- #91 `mango-cli 发布物料与本地版本锁存在不一致风险`

## Risks

- Generated CRUD currently works, but audit fields `created_by/created_at/updated_by/updated_at` were not filled during real create/update. This blocks calling the generated CRUD fully compliant with Mango persistence audit expectations.
- Local CLI is `1.0.20`, but private registry `@mango/cli` is `1.0.19`.
- Local `@mango/admin` is `1.0.12`, while generated project and private registry currently use `1.0.11`.
- Local `@mango/admin-shell` is `1.0.11`, while generated project and private registry currently use `1.0.10`.

## Conclusion

The `mango-cli` local source can initialize an enterprise project and generate a real MyBatis-Plus + Mango CRUD module with complete page/controller/service/mapper structure. The generated CRUD can compile, start, and pass real browser create/query/detail/edit/delete validation.

Not fully passed for enterprise promotion because audit auto-fill and published material version consistency need follow-up.
