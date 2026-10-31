# 按钮权限不显示排障

## 1. 适用场景

用户能打开页面，但新增、编辑、删除、导出、审批等按钮不显示或点击后返回无权限。

> Issue #1083 影响说明：页面菜单默认开启路由缓存，只影响页签切换后的页面状态；按钮权限码、API 权限和授权资源均未变化。

> Issue #983 影响说明：本次调整不改变按钮权限、API 权限或授权资源；文件上传超时错误仍沿用现有权限链路。

## 2. 阅读顺序

| 顺序 | 文档                                                                                     | 关注点                       |
| ---- | ---------------------------------------------------------------------------------------- | ---------------------------- |
| 1    | [Access 后端 README](../../../mango/mango-platform/mango-access/README.md)               | 接口权限、上下文、拦截边界   |
| 2    | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 权限资源、角色授权、菜单关系 |
| 3    | [@mango/rbac README](../../../mango-ui/packages/rbac/README.md)                          | 前端授权数据和管理页面       |
| 4    | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md)            | 登录后权限集合和按钮指令     |

## 3. 接入检查点

| 环节         | 检查点                                                           |
| ------------ | ---------------------------------------------------------------- |
| 按钮资源     | 后端资源中存在按钮 permissionCode                                |
| 菜单关系     | 菜单资源与按钮权限存在父子关系                                   |
| 角色授权     | 角色管理「分配权限」弹框能看到按钮节点，且角色已绑定对应按钮权限 |
| 用户上下文   | 当前用户拥有该角色，且角色在当前租户和组织上下文内生效           |
| 登录态权限   | 登录后权限集合包含目标 permissionCode                            |
| 按钮展示规则 | 登录后 `buttonRules` 中目标按钮的 `displayRule` 执行结果为显示   |
| 前端判断     | 前端按钮使用的权限码与后端资源一致                               |
| 接口校验     | 接口层权限校验与前端按钮权限码一致或有清晰映射                   |

## 4. 最小闭环

1. 给测试角色授权目标菜单和按钮。
2. 用测试用户重新登录。
3. 打开页面确认按钮出现。
4. 点击按钮并确认接口返回业务成功或明确的业务校验错误。
5. 取消按钮授权后重新登录，按钮不可见或接口返回无权限。

## 5. 常见失败

| 现象                     | 优先检查                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------ |
| 按钮完全不显示           | 前端 `v-auth` 或权限判断使用的 permissionCode                                              |
| 按钮显示但接口 403       | 接口权限注解、Access 上下文、后端授权集合                                                  |
| 管理员可见普通用户不可见 | 角色授权、用户角色绑定、组织/岗位限制                                                      |
| 菜单可见按钮不可见       | 菜单授权和按钮授权是否分开配置                                                             |
| 有按钮权限但按钮仍隐藏   | 按钮展示规则、`v-auth` 对象写法和页面传入的 `row` / `pageState` / `query` / `selectedRows` |
| 角色授权弹框看不到按钮   | 后端可分配菜单树是否返回按钮节点、按钮资源是否挂在页面菜单下                               |
| 重新授权后仍不生效       | 登录态权限缓存、前端刷新、token 重新获取                                                   |

## 6. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-access,mango-platform/mango-authorization -am test
pnpm -F @mango/rbac build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Access 验证方式](../../../mango/mango-platform/mango-access/README.md)
- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md)
- [RBAC Frontend 验证方式](../../../mango-ui/packages/rbac/README.md)
- [Admin Shell 验证方式](../../../mango-ui/packages/admin-shell/README.md)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)

## 8. 历史变更

详细版本影响见[按钮权限排障历史索引](../../changelog/business-integration/permission-button-troubleshooting.md)。

> Issue #1098 影响说明：资源声明的 `sync-mode` 收敛为 `SYNC_ALWAYS` / `SYNC_ONCE`（旧值 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 仍可读取并映射）。本场景涉及的数据初始化、租户边界、权限判定与页面行为不变。

> Issue #1098 影响说明：资源登记行新增 `ownership`，`MANUAL_TAKEOVER` 与 `UNKNOWN` 行不再被声明覆盖或停用；本场景的数据初始化、租户边界与权限判定结果不变。

> Issue #1122 影响说明：`PERMISSION` 接口的权限标识和资源编码默认同值，权限标识页面会将两者合并展示。Controller 扫描结果优先使用 `@Operation.summary` 作为中文接口说明；固定声明与扫描命中同一方法、路径时固定声明优先，按钮权限判定和角色授权流程不变。

> Issue #1110 影响说明：通用资源回收仅在完整成功的 `FINALIZE` 快照中处理缺失资源，并按来源和 ownership 保护人工接管记录。`API_RESOURCE` 目标删除及授权缓存刷新不会改变按钮权限码、菜单授权关系或当前租户权限判定；本 FAQ 的按钮排障步骤保持不变。

> Issue #1134 影响说明：Shell 验证码演示页仅调整验证码生成入口和展示文案，不改变按钮权限码、接口权限或授权资源；本 FAQ 的按钮排障步骤保持不变。

> Issue #1158 影响说明：授权租户型 Resource Handler 明确声明 `tenantId` 执行范围，历史 `tenant_id IS NULL` 登记行在 Resource Registry FINALIZE 时只告警跳过，不调用 Handler、不修改登记行；按钮权限码、接口权限、授权资源和当前租户权限判定排障步骤不变。
