# PasswordPolicyHint

密码策略提示组件，根据输入中的密码实时展示规则通过情况和密码强度（弱/中/强），通常配合密码输入框使用。

## 导入

```ts
import { PasswordPolicyHint } from '@mango/common';
import type { PasswordPolicy } from '@mango/common';
```

`PasswordPolicy` 类型以及 `isPasswordPolicyPassed`、`defaultPasswordPolicy` 等工具函数由 Common 包统一导出。

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `password` | `string` | `''` | 当前密码值，组件为受控展示，自身不包含输入框 |
| `policy` | `PasswordPolicy` | `undefined` | 密码策略；未传时使用内置 `defaultPasswordPolicy` |

## 密码策略

`PasswordPolicy` 结构如下：

```ts
interface PasswordPolicy {
  minLength: number; // 最小长度
  requireLetter: boolean; // 是否要求包含字母
  requireDigit: boolean; // 是否要求包含数字
  requireSpecialChar: boolean; // 是否要求包含特殊字符
  allowWhitespace: boolean; // 是否允许空白字符
  pattern?: string; // 可选自定义正则（字符串形式）
}
```

内置默认策略 `defaultPasswordPolicy`：

```ts
{
  minLength: 8,
  requireLetter: true,
  requireDigit: true,
  requireSpecialChar: false,
  allowWhitespace: false,
  pattern: '',
}
```

规则区按策略动态生成：始终展示「至少 N 位」；`requireLetter` 展示「包含字母」；`requireDigit` 展示「包含数字」；`requireSpecialChar` 展示「包含特殊字符」；`allowWhitespace` 为 `false` 时展示「不能包含空白字符」；配置了 `pattern` 时展示「符合自定义规则」。

强度分为未输入、弱、中、强四档文案与三格进度条：密码为空显示「未输入」；未通过全部规则或得分不超过 2 为「弱」；得分不超过 4 为「中」；更高为「强」。得分由已通过规则数与字符种类（小写、大写、数字、特殊字符）综合计算。

组件无 Events、无 Expose、无 Slots，也没有独立的 `types.ts`。

## 使用示例

### 配合密码输入框使用

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { PasswordPolicyHint } from '@mango/common';

const password = ref('');
</script>

<template>
  <el-input v-model="password" type="password" show-password placeholder="请输入密码" />
  <PasswordPolicyHint :password="password" />
</template>
```

### 自定义密码策略

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { PasswordPolicyHint } from '@mango/common';
import type { PasswordPolicy } from '@mango/common';

const password = ref('');

const policy: PasswordPolicy = {
  minLength: 12,
  requireLetter: true,
  requireDigit: true,
  requireSpecialChar: true,
  allowWhitespace: false,
  pattern: '',
};
</script>

<template>
  <el-input v-model="password" type="password" show-password />
  <PasswordPolicyHint :password="password" :policy="policy" />
</template>
```

### 全部规则通过后才允许提交

```vue
<script setup lang="ts">
import { computed, ref } from 'vue';
import { PasswordPolicyHint, isPasswordPolicyPassed } from '@mango/common';

const password = ref('');
const passed = computed(() => isPasswordPolicyPassed(password.value));
</script>

<template>
  <el-input v-model="password" type="password" show-password placeholder="请输入密码" />
  <PasswordPolicyHint :password="password" />
  <el-button type="primary" :disabled="!passed" style="margin-top: 12px">
    提交
  </el-button>
</template>
```

## 重要约束

- 组件只负责展示，不渲染输入框，需要宿主自行绑定 `el-input` 并把值传给 `password`
- 规则文案和强度文案（「密码强度」「弱/中/强」「未输入」等）为组件内置中文，不支持通过 prop 或 i18n 覆盖
- `policy` 为可选 prop，缺省时始终回退到内置默认策略（至少 8 位、含字母、含数字、不允许空白字符）；策略变化会实时重新计算
- `pattern` 以字符串形式传入，组件内部通过 `new RegExp(pattern)` 构造，需自行保证正则合法
- 组件不抛出事件、不暴露方法、不提供插槽，提交前校验请配合 `@mango/common` 导出的 `isPasswordPolicyPassed(password, policy?)`

## 概览

密码输入场景的辅助提示，实时反馈每条策略规则的通过状态和整体密码强度。

## 功能清单

默认与自定义密码策略、规则逐条通过标识、弱/中/强三档强度文案与三格强度条，策略随 props 实时更新。

## 接入方式

从 `@mango/common` 导入 `PasswordPolicyHint`，放置在密码输入框下方，将密码值绑定到 `password`。

## 配置说明

通过 `policy` 配置最小长度、字母/数字/特殊字符要求、空白字符策略与自定义正则；不传则使用默认策略。

## API 与扩展

仅包含 `password`、`policy` 两个 props，无事件、方法和插槽；策略计算工具由 `@mango/common` 的 `utils/passwordPolicy` 统一导出。

## 数据与初始化

无数据初始化要求，所有计算基于当前 `password` 和 `policy` 实时派生。

## 管理入口

无独立管理入口，由注册、修改密码等表单页面接入。

## 快速开始

参见「配合密码输入框使用」示例，传入密码值即可看到提示。

## 问题排查

- 强度条始终为空：确认 `password` 已正确绑定实际输入值而非固定空串
- 规则数量与预期不符：规则由 `policy` 各开关派生，特殊字符规则需 `requireSpecialChar: true`，空白字符规则在 `allowWhitespace: false` 时出现
- 需要和后端一致的校验口径：复用 `isPasswordPolicyPassed` 做提交前判断，保证页面提示与拦截逻辑同源
- 使用自定义正则报错：检查 `pattern` 字符串能否被 `new RegExp` 正常构造

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
