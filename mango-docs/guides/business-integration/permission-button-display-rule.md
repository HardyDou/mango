# 按钮展示规则接入说明

## 适用场景

按钮展示规则用于在已有按钮权限基础上，根据页面数据动态控制按钮是否显示。例如“提交审批”只在草稿状态显示，“批量删除”只在已选中数据时显示。

该能力只控制前端 UI 展示，不替代后端接口鉴权。所有真实操作仍必须由后端接口权限校验兜底。

## 数据来源

按钮规则维护在菜单管理的按钮节点上，仅 `menuType = 3` 时生效。

| 字段 | 含义 | 说明 |
| --- | --- | --- |
| `menuCode` | 权限标识 | 前端 `v-auth` 的 `code` 使用这个值，按钮规则也按这个值匹配 |
| `permissions` | 接口标识 | 用于记录页面或按钮关联的接口权限标识，不参与按钮展示规则匹配 |
| `buttonType` | 按钮类型 | `TABLE` 表格按钮，`NON_TABLE` 非表格按钮，非必填 |
| `buttonDisplayRule` | 按钮展示规则 | JavaScript 表达式，最长 1000 字符，非必填 |

用户登录或刷新用户信息时，后端会返回当前用户已授权按钮的规则：

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

其中 `buttonRules[].code` 与菜单按钮节点的 `menuCode` 保持一致。空规则默认显示；规则执行异常时隐藏按钮。

## 前端使用

普通按钮权限仍沿用字符串写法：

```vue
<el-button v-auth="'workflow:todo:add'">新增</el-button>
```

需要动态展示规则时使用对象写法：

```vue
<el-button
  v-auth="{ code: 'workflow:todo:submit', row }"
>
  提交审批
</el-button>
```

可传入的规则上下文如下：

| 字段 | 含义 | 典型用途 |
| --- | --- | --- |
| `row` | 当前行数据 | 表格行按钮按状态显示 |
| `pageState` | 页面状态 | 根据只读、流程阶段等页面状态显示 |
| `query` | 查询条件 | 根据筛选条件显示顶部按钮 |
| `selectedRows` | 已选行集合 | 批量按钮按选择数量显示 |

示例规则：

```js
row.status === 'DRAFT'
row.status !== 'APPROVING' && row.amount > 0
!pageState.readonly
query.status === 'PENDING'
selectedRows.length > 0
```

## 角色分配

角色管理的“分配权限”弹框会正常展示菜单数据中的按钮节点。按钮节点可额外显示“表格按钮”或“非表格按钮”标记，名称包含“列表”的按钮不展示类型标记。

是否拥有某个按钮仍由角色绑定的菜单按钮节点决定；展示规则只在拥有按钮权限后继续判断是否显示。

## 注意事项

- 按钮展示规则使用 `new Function` 执行，只允许管理员在菜单管理中维护。
- 展示规则只能控制显示或隐藏，不控制禁用态；禁用逻辑由业务页面自行处理。
- 修改菜单按钮规则后，用户需要重新登录或重新获取 `/auth/info`，前端才能拿到最新规则。
- 不要在规则里写副作用逻辑，例如修改 `row`、发请求、操作全局状态。
