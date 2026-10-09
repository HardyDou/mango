# Captcha

验证码组件集。`CaptchaSelector` 统一选择/承载算术、图片滑块、点选文字、无感行为、Canvas 滑块、短信、邮件七种验证方式，七个子组件也可单独使用。

## 导入

```ts
import {
  CaptchaSelector,
  ArithmeticCaptcha,
  BlockPuzzleCaptcha,
  ClickWordCaptcha,
  BehaviorCaptcha,
  CanvasSliderCaptcha,
  SmsCaptcha,
  EmailCaptcha,
  CaptchaType,
} from '@mango/common';
import type {
  CaptchaResponse,
  CaptchaVerifyRequest,
  BehaviorCaptchaVerifyResult,
  CaptchaTypesResponse,
} from '@mango/common';
```

## CaptchaSelector

### Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | `CaptchaType` | `undefined` | 固定验证码类型。传入后只渲染对应子组件；不传时渲染七个页签供用户切换，默认选中 `CaptchaType.CANVAS_SLIDER` |
| `mode` | `'embedded' \| 'trigger' \| 'popup'` | `'embedded'` | 展示模式。仅向 `BLOCK_PUZZLE`、`CANVAS_SLIDER` 两个滑块子组件透传 |

展示模式说明：

- `embedded`：内嵌在页面中直接展示完整验证区域。
- `trigger`：触发式，仅滑块类子组件生效（拼图为悬停浮层、Canvas 滑块为点击气泡）。
- `popup`：以 Element Plus 弹框渲染，标题固定为“请完成安全验证”；弹框宽度滑块拼图/Canvas 滑块为 `340px`、点选文字为 `380px`、其余为 `420px`。算术、无感行为、短信、邮件在弹框底部渲染“取消 / 确认”按钮。

### Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `success` | `key: string, code?: string, type?: CaptchaType` | 任一子组件验证通过时触发，`type` 为当前验证方式；算术和无感行为会携带 `code` |
| `refresh` | - | 子组件刷新题目时触发（短信、邮件子组件不触发） |
| `inputChange` | `value: string, type?: CaptchaType` | 算术验证码输入内容变化时触发 |

### Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `refresh()` | - | - | 重置已通过状态并刷新当前子组件题目 |
| `verify()` | - | `Promise<boolean>` | 主动校验。`popup` 模式下会打开弹框，用户通过、点击确认或关闭弹框后 resolve；非弹框模式委托当前子组件的 `verify()` |

## 子组件共享契约

七个子组件挂载后即自动准备题目（短信、邮件除外，需先填写目标并点击发送），并对外保持一致的方法与成功事件：

| 契约项 | 签名 | 说明 |
|--------|------|------|
| `success` 事件 | `(key: string, code?: string) => void` | 验证通过时抛出后端/本地会话 `key`；算术、无感行为额外携带 `code` |
| `refresh()` 方法 | - | 重新生成题目并清空输入、错误与通过状态 |
| `verify()` 方法 | `() => Promise<boolean>` | 主动触发校验并返回是否通过 |
| 自动初始化 | - | 挂载时自动调用 `refresh()`（短信、邮件除外） |

## 子组件差异

| 子组件 | `CaptchaType` | Props | 后端端点 | 交互与校验载荷 | 额外 Events |
|--------|---------------|-------|----------|----------------|-------------|
| `ArithmeticCaptcha` | `ARITHMETIC` | 无 | `GET /captcha/arithmetic`、`POST /captcha/verify` | 展示计算题图片，输入计算结果，回车提交；校验传 `code` | `refresh`、`inputChange` |
| `BlockPuzzleCaptcha` | `BLOCK_PUZZLE` | `mode?: 'embedded' \| 'trigger' \| 'popup'`（默认 `embedded`） | `GET /captcha/block-puzzle`、`POST /captcha/verify` | 鼠标/触屏拖动拼图块，松手提交；校验传 `pointJson: { x, y }`（按原图尺寸换算）；失败自动换图 | `refresh` |
| `ClickWordCaptcha` | `CLICK_WORD` | 无 | `GET /captcha/click-word`、`POST /captcha/verify` | 按提示依次点击图片文字，点满数量自动提交；校验传 `pointJson: { points: [{ x, y }] }`；支持“重选” | `refresh` |
| `BehaviorCaptcha` | `BEHAVIOR` | `showScore?: boolean`（默认 `false`） | `GET /captcha/behavior`、`POST /captcha/behavior/verify` | 点击按钮完成无感行为验证；采集鼠标轨迹、点击、按键与设备指纹（UA、屏幕、时区、语言、Canvas 指纹等）作为 `pointJson`；失败 900ms 后自动重置 | `refresh` |
| `CanvasSliderCaptcha` | `CANVAS_SLIDER` | `mode?: 'embedded' \| 'trigger' \| 'popup'`（默认 `embedded`） | 无，纯前端校验 | 拖到随机目标位置（80–200px），误差 ≤ 8px 即通过；`key` 为本地生成的 `canvas_<时间戳>_<随机串>`；`trigger` 模式使用 `el-popover` 气泡 | `refresh` |
| `SmsCaptcha` | `SMS` | 无 | `POST /auth/captcha/send`、`POST /captcha/verify` | 手机号正则 `^1[3-9]\d{9}$`，发送后 60 秒倒计时，响应字符串作为 `key`；校验传短信 `code` | 仅 `success` |
| `EmailCaptcha` | `EMAIL` | 无 | `POST /auth/captcha/send`、`POST /captcha/verify` | 邮箱正则 `^[^\s@]+@[^\s@]+\.[^\s@]+$`，发送后 60 秒倒计时，响应字符串作为 `key`；校验传邮件 `code` | 仅 `success` |

短信、邮件发送接口的请求体固定为 `{ type, target, businessType: 'LOGIN' }`，其中 `type` 分别为 `SMS`、`EMAIL`。

## 类型定义

```ts
enum CaptchaType {
  ARITHMETIC = 'ARITHMETIC',
  BLOCK_PUZZLE = 'BLOCK_PUZZLE',
  CLICK_WORD = 'CLICK_WORD',
  BEHAVIOR = 'BEHAVIOR',
  CANVAS_SLIDER = 'CANVAS_SLIDER',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
}

interface CaptchaResponse {
  key: string;
  type: CaptchaType;
  image?: string;            // 算术/点选文字题图（dataURL 或图片地址）
  backgroundImage?: string;  // 拼图背景图
  sliderImage?: string;      // 拼图滑块图
  backgroundWidth?: number;  // 原图宽，用于坐标换算
  backgroundHeight?: number; // 原图高
  sliderSize?: number;       // 滑块原图边长
  x?: number;                // 缺口目标横坐标
  y?: number;                // 缺口目标纵坐标
  expireTime: number;        // 过期时间
  target?: string;           // 点选提示文字，多个以逗号分隔
  extra?: string;            // 点选附加信息 JSON：{ width, height, pointCount }
}

interface CaptchaVerifyRequest {
  key: string;
  type: CaptchaType;
  code?: string;      // 算术/短信/邮件验证码
  pointJson?: string; // 滑块坐标、点选坐标或行为采集数据的 JSON 字符串
}

interface BehaviorCaptchaVerifyResult {
  key: string;
  score: number;
  passed: boolean;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  suggestAction: 'ALLOW' | 'SECONDARY_VERIFY' | 'DENY';
  reason: string;
}

interface CaptchaTypesResponse {
  types: CaptchaType[];
  currentStorage: string;
}
```

后端验证码相关接口汇总（`packages/common/api/captcha.ts`）：

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/captcha/types` | 查询启用的验证码类型与当前存储方式 |
| GET | `/captcha/arithmetic` | 生成算术验证码 |
| GET | `/captcha/block-puzzle` | 生成图片滑块拼图 |
| GET | `/captcha/click-word` | 生成点选文字验证码 |
| GET | `/captcha/behavior` | 初始化无感行为验证会话 |
| POST | `/captcha/behavior/verify` | 行为验证评分，返回 `BehaviorCaptchaVerifyResult` |
| POST | `/captcha/verify` | 统一校验（算术/拼图/点选/短信/邮件），返回 `boolean` |
| POST | `/auth/captcha/send` | 发送短信/邮件验证码，返回验证码 `key` |

## 使用示例

### 固定类型内嵌使用

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CaptchaSelector, CaptchaType } from '@mango/common';

const captchaRef = ref<InstanceType<typeof CaptchaSelector> | null>(null);

function onSuccess(key: string, code?: string, type?: CaptchaType) {
  console.log('验证通过', { key, code, type });
}
</script>

<template>
  <CaptchaSelector
    ref="captchaRef"
    :type="CaptchaType.BLOCK_PUZZLE"
    @success="onSuccess"
  />
</template>
```

### 弹框模式下主动发起校验

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CaptchaSelector, CaptchaType } from '@mango/common';

const captchaRef = ref<InstanceType<typeof CaptchaSelector> | null>(null);

async function submit() {
  const passed = await captchaRef.value?.verify();
  if (!passed) return;
  // 通过后继续业务提交
}
</script>

<template>
  <CaptchaSelector
    ref="captchaRef"
    :type="CaptchaType.BEHAVIOR"
    mode="popup"
  />
  <el-button type="primary" @click="submit">提交</el-button>
</template>
```

### 多方式切换（不传 type）

```vue
<script setup lang="ts">
import { CaptchaSelector } from '@mango/common';

function onSuccess(key: string, code?: string) {
  // key 用于随业务请求提交给后端二次校验
  console.log(key, code);
}
</script>

<template>
  <CaptchaSelector
    @success="onSuccess"
    @refresh="() => console.log('题目已刷新')"
    @input-change="(value) => console.log('输入中', value)"
  />
</template>
```

### 短信验证码单独使用

```vue
<script setup lang="ts">
import { SmsCaptcha } from '@mango/common';

function onSuccess(key: string) {
  console.log('短信验证通过，key =', key);
}
</script>

<template>
  <SmsCaptcha @success="onSuccess" />
</template>
```

## 重要约束

- `CanvasSliderCaptcha` 为纯前端校验，不请求任何后端端点，会话 `key` 也在浏览器本地生成，不能作为安全凭据；对安全要求高的场景使用后端下发并校验的拼图、点选或行为验证。
- `BlockPuzzleCaptcha` 与 `CanvasSliderCaptcha` 的 `verify()` 仅在 `popup` 模式下返回可等待的 Promise；内嵌和触发模式下用户完成拖拽即自动抛出 `success`，主动调用 `verify()` 会立即返回 `false`。
- 短信、邮件的发送场景固定为 `businessType: 'LOGIN'`，组件不支持自定义业务类型；发送按钮依赖手机号/邮箱格式校验，倒计时 60 秒。
- `CaptchaSelector` 不传 `type` 时默认展示 Canvas 滑块页签；切换页签会重置已通过状态。
- 拼图提交的坐标按后端原图尺寸（`backgroundWidth`/`backgroundHeight`）换算，组件依赖容器实际渲染宽度进行缩放，不要在隐藏容器中初始化后直接校验。
- 行为验证在组件挂载期间监听全局 `mousemove`、`click`、`keydown` 用于采集行为数据，卸载时自动移除监听。

## 概览

登录、敏感操作等场景使用的人机验证组件集合，支持一个选择器加七种独立验证形态。

## 功能清单

支持算术、图片滑块拼图、点选文字、无感行为、Canvas 滑块、短信、邮件七种验证；支持内嵌、触发、弹框三种展示模式；提供统一的 `success`、`refresh`、`inputChange` 事件与 `refresh()`、`verify()` 方法。

## 接入方式

从 `@mango/common` 导入 `CaptchaSelector` 或任意子组件，在登录、注册、短信/邮件发送确认等表单中嵌入，并监听 `success` 获取验证 `key`。

## 配置说明

通过 `type` 固定验证方式或留空展示全部页签；通过 `mode` 控制内嵌/触发/弹框形态；行为验证可通过 `showScore` 展示评分面板。题目内容、有效期与开关由后端 `/captcha/*` 接口决定。

## API 与扩展

统一事件为 `success(key, code?, type?)`、`refresh`、`inputChange(value, type?)`，统一方法为 `refresh()` 与 `verify(): Promise<boolean>`；类型与会话结构见 `api/captcha.ts` 中的 `CaptchaResponse`、`CaptchaVerifyRequest`、`BehaviorCaptchaVerifyResult`。

## 数据与初始化

无前端本地数据初始化。子组件挂载后自动调用 `/captcha/*` 生成接口（Canvas 滑块为本地随机出题，短信/邮件需用户主动发送）；会话状态与过期时间由后端通过 `key`、`expireTime` 管理。

## 管理入口

无独立管理入口。后端可通过 `GET /captcha/types` 返回启用的验证类型列表。

## 快速开始

固定类型直接传入 `type`；需要点击按钮后再验证时使用 `mode="popup"` 并调用 `verify()` 等待结果；参见上方使用示例。

## 问题排查

- 验证一直失败：确认后端 `/captcha/verify` 返回 `true`，拼图与点选需检查坐标换算是否基于原图尺寸。
- 弹框模式 `verify()` 一直 pending：用户必须完成验证或主动关闭弹框才会 resolve；滑块类在非 popup 模式调用会立即返回 `false`。
- 短信/邮件按钮不可点：手机号或邮箱格式未通过正则校验；倒计时未结束也会禁用。
- Canvas 滑块通过后后端不认：该组件无后端会话，属预期行为，请改用服务端验证类型。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
