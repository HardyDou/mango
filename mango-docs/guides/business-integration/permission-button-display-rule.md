# 按钮展示规则接入

按钮展示规则是在已有按钮权限之上，根据页面数据动态控制 UI 是否显示。例如“提交审批”只在草稿状态显示，“批量删除”只在已选择数据时显示。

> 展示规则只影响前端显示，不替代后端接口鉴权。真实操作仍由后端权限和业务状态校验兜底。

## 1. 适用场景

适用于按钮本身已经有稳定权限码，但是否展示还取决于当前行、页面状态、查询条件或选择集合的业务页面。

不适用于：

- 替代角色、菜单或接口权限；
- 实现复杂业务计算或副作用；
- 用前端隐藏按钮代替后端安全校验。

## 2. 接入前提

1. 菜单管理中已经存在 `menuType = 3` 的按钮节点。
2. 按钮节点的 `menuCode` 与后端权限码、前端 `v-auth` 的 `code` 一致。
3. 角色已授权所属页面和按钮，测试用户能通过 `/auth/info` 获取最新权限。
4. 后端接口仍执行独立的权限、租户、数据范围和业务状态校验。

## 3. 规则数据

按钮规则维护在菜单管理的按钮节点上：

| 字段 | 含义 | 说明 |
| --- | --- | --- |
| `menuCode` | 按钮权限标识 | 前端 `v-auth` 的 `code` 使用此值 |
| `permissions` | 关联接口标识 | 记录接口权限关系，不参与展示表达式匹配 |
| `buttonType` | 按钮类型 | `TABLE` 或 `NON_TABLE`，非必填 |
| `buttonDisplayRule` | 展示表达式 | 最长 1000 字符，空值表示默认显示 |

登录或刷新用户信息时，服务端返回当前用户已授权按钮的规则：

```json
{
  "permissions": ["workflow:todo:submit"],
  "buttonRules": [
    {
      "code": "workflow:todo:submit",
      "buttonType": "TABLE",
      "displayRule": "row.status === 'DRAFT'"
    }
  ]
}
```

空规则默认显示；规则执行异常时隐藏按钮。

## 4. 前端接入

普通按钮继续使用字符串写法：

```vue
<el-button v-auth="'workflow:todo:add'">新增</el-button>
```

需要动态上下文时使用对象写法：

```vue
<el-button v-auth="{ code: 'workflow:todo:submit', row }">
  提交审批
</el-button>
```

可用上下文：

| 字段 | 含义 | 典型用途 |
| --- | --- | --- |
| `row` | 当前行数据 | 按行状态显示操作 |
| `pageState` | 页面状态 | 按只读态或流程阶段显示 |
| `query` | 当前查询条件 | 按筛选条件显示顶部操作 |
| `selectedRows` | 已选行集合 | 按选择数量显示批量操作 |

示例表达式：

```js
row.status === 'DRAFT'
row.status !== 'APPROVING' && row.amount > 0
!pageState.readonly
query.status === 'PENDING'
selectedRows.length > 0
```

## 5. 角色与刷新

角色管理的“分配权限”弹框应能看到按钮节点。角色先获得页面/菜单和按钮授权，展示规则再进行第二次判断。

修改按钮规则或角色授权后，测试用户需要重新登录或重新获取 `/auth/info`，确认登录态权限集合已经刷新。不能只刷新页面而假定旧 token 已包含新规则。

## 6. 最小验收闭环

1. 给测试角色授权页面和目标按钮。
2. 使用满足规则的页面数据，确认按钮显示并可调用接口。
3. 使用不满足规则的数据，确认按钮隐藏。
4. 直接调用接口验证：无后端授权时即使绕过 UI 也返回无权限。
5. 修改规则后重新获取登录态，确认新规则生效。

## 7. 失败边界

- 只支持 `row`、`pageState`、`query`、`selectedRows` 的属性读取，以及 `!`、比较、`&&`、`||` 和括号。
- 不支持函数调用、赋值、数组下标、全局对象访问或副作用逻辑；解析失败时隐藏按钮。
- 规则只能控制显示/隐藏，不控制禁用态；禁用逻辑由业务页面实现。
- 按钮不显示时，先看 [按钮权限不显示排障](../faq/permission-button-troubleshooting.md)；接口返回 403 时以服务端授权结果为准。

## 8. 关联入口

- [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md)
- [Access 后端 README](../../../mango/mango-platform/mango-access/README.md)
- [@mango/rbac README](../../../mango-ui/packages/rbac/README.md)
- [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
