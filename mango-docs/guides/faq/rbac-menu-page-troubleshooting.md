# 菜单页面打不开排障

## 1. 适用场景

用户登录后能看到菜单，但点击菜单出现空白页、404、组件加载失败或接口无权限。

> Issue #1083 影响说明：页面菜单默认开启路由缓存（`keep_alive=1`），不改变菜单可见性、页面 key 或授权资源；菜单打不开时仍按本页逐项排查。

> Issue #983 影响说明：本次调整不改变菜单、页面注册或权限资源；文件上传超时能力不会改变 RBAC 排障步骤。

## 2. 阅读顺序

| 顺序 | 文档                                                                                     | 关注点                         |
| ---- | ---------------------------------------------------------------------------------------- | ------------------------------ |
| 1    | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、权限、资源同步、授权关系 |
| 2    | [@mango/rbac README](../../../mango-ui/packages/rbac/README.md)                          | RBAC 前端包和 API              |
| 3    | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md)                 | 页面 key 和组件映射            |
| 4    | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md)            | 页面注册、菜单渲染、登录后装配 |

## 3. 接入检查点

| 环节       | 检查点                                                                   |
| ---------- | ------------------------------------------------------------------------ |
| 菜单数据   | `/authorization/menus/user?fmt=tree&appCode=internal-admin` 返回目标菜单 |
| 页面 key   | 菜单 `component` 字段能匹配前端页面 key                                  |
| 前端注册   | 前端包已引入并完成页面注册或路由映射                                     |
| 角色授权   | 当前用户角色已绑定目标菜单                                               |
| 租户绑定   | 当前租户已绑定目标应用和菜单包                                           |
| 运行态请求 | 浏览器 network 中页面依赖和业务接口没有未解释的 401/403/404              |

## 4. 最小闭环

1. 用目标用户登录。
2. 打开菜单接口，确认返回目标菜单和 component key。
3. 在前端页面 key 文档中确认 component key 存在。
4. 点击菜单，页面组件正常加载。
5. 浏览器 network 中页面资源、菜单接口和业务接口没有未解释的 401/403/404。

## 5. 可选运行态诊断

`mango module doctor mango-link` 是默认关闭、只读的本机诊断入口，可交叉检查 starter、Flyway、Resource 声明、Authorization 菜单/API 物化和页面 loader/chunk。它不修改授权数据，也不能替代本指南的用户级菜单、角色、页面注册和浏览器 Network 检查；其它模块或证据不可观察时应按 UNKNOWN 处理。

## 6. 页面 key 对照

| 能力     | 常见页面 key 文档                                                                          |
| -------- | ------------------------------------------------------------------------------------------ |
| Auth     | [Auth Views README](../../../mango-ui/packages/auth/src/views/README.md)                   |
| File     | [File Components README](../../../mango-ui/packages/file/src/components/README.md)         |
| Job      | [Job Views README](../../../mango-ui/packages/job/src/views/README.md)                     |
| RBAC     | [RBAC Views README](../../../mango-ui/packages/rbac/src/views/README.md)                   |
| System   | [System Components README](../../../mango-ui/packages/system/src/components/README.md)     |
| Workflow | [Workflow Components README](../../../mango-ui/packages/workflow/src/components/README.md) |

## 7. 常见失败

| 现象               | 优先检查                                            |
| ------------------ | --------------------------------------------------- |
| 菜单存在但点击空白 | component key 与前端注册表不一致                    |
| 菜单不存在         | resource manifest、迁移 SQL、角色授权和租户应用绑定 |
| 页面加载但接口 403 | Access、Authorization 和当前用户权限集合            |
| 刷新后页面丢失     | 前端路由 fallback、admin-shell 注册时机             |
| 只有某租户异常     | 租户应用绑定、菜单包绑定、租户初始化数据            |

## 8. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization -am test
pnpm -F @mango/rbac build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md)
- [RBAC Frontend 验证方式](../../../mango-ui/packages/rbac/README.md)
- [Admin Shell 验证方式](../../../mango-ui/packages/admin-shell/README.md)

## 9. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 10. 历史变更

详细版本影响见[菜单页面排障历史索引](../../changelog/business-integration/rbac-menu-page-troubleshooting.md)。

## 11. Issue #1055 影响说明

信息采集新增 `information/forms/index`、`information/submissions/index` 与 `information/hosted/index` 三个页面 key，并新增一个免登录公开路由 `/information/hosted`。菜单页面排障流程、既有页面 key 对照关系和后端菜单同步机制均不变；排查信息采集页面时，请确认宿主应用已注册 `registerMangoInformationAdminPages` 且样式包已加载。

> Issue #1098 影响说明：资源声明的 `sync-mode` 收敛为 `SYNC_ALWAYS` / `SYNC_ONCE`（旧值 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 仍可读取并映射）。本场景涉及的数据初始化、租户边界、权限判定与页面行为不变。

> Issue #1098 影响说明：资源登记行新增 `ownership`，`MANUAL_TAKEOVER` 与 `UNKNOWN` 行不再被声明覆盖或停用；本场景的数据初始化、租户边界与权限判定结果不变。

> Issue #1122 影响说明：新增“系统管理 / 权限管理 / 权限标识”页面，页面 key 为 `system/api-resource/index`，接口权限码为 `system:api-resource:list`。原有菜单、页面 key、角色授权和租户绑定流程不变；同一 API 只保留一条有效登记，固定声明优先于 Controller 扫描。
