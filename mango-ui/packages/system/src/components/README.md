# System Components

## 1. 入口定位

本入口说明 `@mango/system` 的复用选择器组件：参与人选择器、业务域选择器和业务域侧边树。它们用于业务页面选择用户、组织范围、角色、岗位或业务域。

## 2. 公开导出

来自 `@mango/system`：

- `ParticipantSelector`
- `DomainSelector`
- `DomainSideTree`
- `ParticipantOrgTreeOption`
- `ParticipantSelectorLoading`
- `ParticipantSelectorValue`
- `ParticipantTargetOption`
- `ParticipantType`

## 3. 使用场景

- 工作流节点配置选择用户、部门范围、角色或岗位。
- 业务表单选择参与人、处理人、抄送对象或授权对象。
- 按业务域筛选模板、流程、配置或业务数据。
- 后台列表页通过业务域树进行左侧过滤。

## 4. 接入方式

```ts
import { ParticipantSelector, DomainSelector, DomainSideTree } from '@mango/system';
import '@mango/system/style.css';
```

参与人选择：

```vue
<ParticipantSelector v-model="participants" />
```

`ParticipantSelectorValue` 结构：

```ts
const participants = ref({
  userIds: ['10001'],
  orgIds: ['20001'],
  roleIds: ['30001'],
  postIds: ['40001'],
});
```

候选项结构：

```ts
const userOptions = [
  { label: '张三 / zhangsan', value: '10001' },
];

const orgTreeOptions = [
  {
    label: '研发中心',
    value: '20001',
    children: [{ label: '平台组', value: '20002' }],
  },
];
```

外部加载候选项：

```vue
<ParticipantSelector
  v-model="participants"
  :user-options="userOptions"
  :role-options="roleOptions"
  :post-options="postOptions"
  :org-tree-options="orgTreeOptions"
  :target-loading="targetLoading"
  @ensure-users="loadUsers"
  @ensure-roles="loadRoles"
  @ensure-posts="loadPosts"
  @ensure-orgs="loadOrgs"
/>
```

未传 `userOptions` 时，用户页签会按 `/identity/users/page` 加载前 200 条用户；角色、岗位、组织候选项由调用方通过 `ensure-*` 事件加载。

业务域选择：

```vue
<DomainSelector v-model="domainId" />
```

业务域侧边树：

```vue
<DomainSideTree v-model="domainCode" @change="loadRows" />
```

## 5. Props / 参数 / 事件

`ParticipantSelector` props：

- `modelValue`：`ParticipantSelectorValue`。
- `userOptions`、`roleOptions`、`postOptions`、`orgTreeOptions`：候选数据。
- `targetLoading`：候选数据加载态。
- `placeholder`、`searchPlaceholder`。

`ParticipantSelector` 事件：

- `update:modelValue`
- `ensure-users`
- `ensure-roles`
- `ensure-posts`
- `ensure-orgs`

`DomainSelector` props：

- `modelValue`
- `multiple`
- `clearable`
- `disabled`
- `checkStrictly`
- `placeholder`

`DomainSelector` 事件：

- `update:modelValue`
- `change`

`DomainSideTree` props：

- `modelValue`
- `title`
- `subtitle`
- `allLabel`
- `allCode`
- `allCount`
- `counts`
- `options`
- `searchable`
- `searchPlaceholder`
- `showAll`

`DomainSideTree` 事件：

- `update:modelValue`
- `change`
- `loaded`

## 6. 后端依赖

- 后端模块：`mango-platform/mango-system`、`mango-platform/mango-identity`、`mango-platform/mango-org`、`mango-platform/mango-authorization`。
- `DomainSelector` 和 `DomainSideTree` 使用 `domainApi.enabledTree()`，对应业务域启用树接口。
- `ParticipantSelector` 可使用外部传入候选项，也可在打开用户页签时通过 `@mango/common` 请求用户分页数据。

## 7. 权限 / 租户 / 数据边界

- 业务域、用户、组织、角色、岗位候选数据由后端按租户和权限过滤。
- 选择器只返回 id/code 集合，不代表最终授权结果。
- 调用方提交业务数据时仍由后端接口校验权限、租户和数据归属。

## 8. 验证方式

```bash
pnpm -F @mango/system build
```

页面验收入口：

- 系统业务域管理页面。
- 使用 `ParticipantSelector` 的流程或业务配置页面。

最小断言：

- `ParticipantSelector` 能选择并回显用户、部门范围、角色和岗位。
- `DomainSelector` 能加载启用业务域树并回写 id。
- `DomainSideTree` 能加载业务域树、搜索并触发 `change`。

## 9. 常见问题

- 参与人名称不回显时，检查传入候选项是否包含已选 id。
- 业务域树为空时，检查后端业务域是否启用。
- 选择器返回值正确但业务提交失败时，继续检查业务接口权限和租户。

## 10. 关联文档

- [@mango/system README](../../README.md)
- [System 后端 README](../../../../../mango/mango-platform/mango-system/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Org 后端 README](../../../../../mango/mango-platform/mango-org/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
