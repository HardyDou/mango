---
title: "feat: mango P2 组件前后端实现计划"
type: feat
status: active
date: 2026-04-01
origin: docs/plans/2026-03-31-001-feat-mango-web-p2-components-plan.md, docs/frontend-backend-chat.md
---

# mango P2 组件前后端实现计划

## Overview

本计划实现 P2 阶段 7 个核心组件，**按前后端明确分工，支持 AI Agent 并行协作**。

## 分工总览

> **FE/A 编号说明**: FE1=useTitle, FE2=Sign, FE3=ChinaArea, FE4=OrgSelector, FE5=SSE, FE6=WebSocket, FE7=Chat; A1-A5 为后端接口 (/admin/sysArea/tree, /admin/sysOrg/tree, /admin/sse/connect, /admin/ws/chat, /admin/ai/chat)

```mermaid
graph LR
    subgraph Frontend(🍋 前端)
        FE1["useTitle"]
        FE2["Sign"]
        FE3["ChinaArea"]
        FE4["OrgSelector"]
        FE5["SSE"]
        FE6["WebSocket"]
        FE7["Chat"]
    end

    subgraph Backend(☕ 后端接口)
        A1["/mango/area/tree"]
        A2["/mango/org/tree"]
        A3["/mango/sse/connect"]
        A4["/mango/ws/chat"]
        A5["/mango/ai/chat"]
    end

    FE3 --> |依赖| A1
    FE4 --> |依赖| A2
    FE5 --> |依赖| A3
    FE6 --> |依赖| A4
    FE7 --> |依赖| A5
    FE7 -.-> |可选依赖| FE5

    style Frontend fill:#fef3c7
    style Backend fill:#dbeafe
```

## 前端依赖

| 编号 | 组件 | 参考来源 | 状态 |
|------|------|---------|------|
| FE1 | useTitle | 自研 | ⚠️ 待实现 |
| FE2 | Sign | 参考 pigx-ui | ⚠️ 待适配 |
| FE3 | ChinaArea | 参考 pigx-ui | ⚠️ 待适配 |
| FE4 | OrgSelector | 参考 pigx-ui | ⚠️ 待适配 |
| FE5 | SSE | 参考 pigx-ui | ⚠️ 待适配 |
| FE6 | WebSocket | 原生封装 | ⚠️ 待实现 |
| FE7 | Chat | 参考 pigx-ui | ⚠️ 待适配 |

> 所有组件参考 pigx-ui 实现，前端 Agent 自行决定实现细节

## 后端接口清单

| 接口 | 路径 | 用途 | 依赖方 |
|------|------|------|--------|
| 行政区划树 | `/mango/area/tree` 或 `/bff/admin/area/tree` | ChinaArea | 前端 Unit B3 |
| 组织架构树 | `/mango/org/tree` 或 `/bff/admin/org/tree` | OrgSelector | 前端 Unit B4 |
| SSE 推送 | `/mango/sse/connect` 或 `/bff/admin/sse/connect` | SSE 组件 | 前端 Unit B5 |
| WebSocket | `/mango/ws/chat` 或 `/bff/admin/ws/chat` | WebSocket 组件 | 前端 Unit B6 |
| AI 对话 | `/mango/ai/chat` 或 `/bff/admin/ai/chat` | Chat 组件 | 前端 Unit B7 |

> **路径说明**: 独立服务模式用 `/mango/*`，BFF 代理模式用 `/bff/admin/*`。由后端 Agent 根据模块定位选择。

## Implementation Units

---

### Phase A: 后端接口 (可与前端并行)

---

- [ ] **Unit A1: 行政区划树接口**

**Goal:** 实现省市区街道四级联动数据接口

**Dependencies:** 无

**Files:**
- Create: `mango/mango-area/` → 后端模块 (由后端 Agent 实现)
  - `mango-area-api/` → 实体、VO
  - `mango-area-core/` → Service、Mapper (查询已有 sys_area 表)
  - `mango-area-starter/` → Starter

**路径策略:**
- **方案A (推荐)**: 独立服务模式 → 暴露路径 `/mango/area/tree`
- **方案B**: BFF 代理模式 → 内部 `/admin/area/tree`，网关代理 `/bff/admin/area/tree` → `/admin/area/tree`
- 由后端 Agent 根据 mango 模块定位选择（独立服务 vs 集成到 pigx 网关）

**Approach:**
- 后端返回树形结构，含 adcode、name、level、hot、children
- 支持 type 参数过滤层级 (1-省, 2-市, 3-区/县, 4-街道)
- **懒加载模式**: 按需加载，点击展开时请求子节点
- 支持 parentId 参数按需加载子节点
- 展开层级：省→市→区/县→街道，按需加载每级全部子节点

**数据导入:**
- 数据来源: `mango/mango-area/sql/V1__mango_area_init.sql` (已有全量省市区街道数据)
- 数据已导入至 `sys_area` 表，无需重复导入
- **注意**: 不使用 data.sqlite，直接使用现有 SQL 文件

**Patterns to follow:**
- 现有 `SysI18n` 模块结构
- MyBatis-Plus TreeQueryWrapper

**租户隔离:**
- 行政区划数据通常为公共基础数据，多租户共享
- 如需按租户隔离，参考 `mango-i18n-core/` 的 `TenantLineHandler` 多租户隔离实现；当前设计为公共基础数据，多租户共享，无需隔离

**Test scenarios:**
- 部署后 curl 验证返回树形结构
- 验证 children 递归正确
- 点击展开省节点，验证只返回该省下的市而非全国数据
- 懒加载按 parentId 查询，验证只返回指定父节点的直接子节点

**Verification:**
- `GET /mango/area/tree` (或 `/bff/admin/area/tree`) 返回根节点 + 第一层子节点 (懒加载首次返回两层)
- `GET /mango/area/tree?parentId=440000` 只返回广东省下级 (懒加载按需返回)
- `GET /mango/area/tree?type=2` 返回市级数据
- 懒加载验证: 展开省节点后，验证只返回该省下的市而非全国数据

---

- [ ] **Unit A2: 组织架构树接口**

**Goal:** 实现组织选择器数据接口

**Dependencies:** 无

**Files:**
- Create: `mango/mango-org-tree/` → 组织树模块 (由后端 Agent 实现)
  - `mango-org-tree-api/` → VO (独立于 mango-org 避免耦合)
  - `mango-org-tree-core/` → Service、Mapper (查询复用 mango_org 表)
  - `mango-org-tree-starter/` → Starter

**决策:** 复用 `mango_org` 表数据，不复用 `mango-org` 模块代码。新建独立封装模块避免循环依赖。

**Approach:**
- 返回树形组织结构
- 支持 parentId 查询子级 (默认 parentId=0 查根节点)
- **懒加载模式**: 点击展开时按需加载子节点
- 按 tenant_id 隔离租户数据
- **接口契约** (见 frontend-backend-chat.md §2)

**Patterns to follow:**
- 现有权限模块结构

**租户隔离:**
- 通过 tenant_id 字段隔离
- 根节点查询需过滤对应租户

**Test scenarios:**
- 验证根节点返回
- 验证子节点递归

**Verification:**
- `GET /mango/org/tree` (或 `/bff/admin/org/tree`) 返回组织树

---

- [ ] **Unit A3: SSE 推送接口**

**Goal:** 实现 SSE 服务端推送

**Dependencies:** 无

**Files:**
- Create: `mango/mango-sse/` → SSE 模块
  - `mango-sse-core/` → SseEmitter 配置、消息推送 Service

**Approach:**
- 使用 Spring `SseEmitter` 实现
- 支持认证头 `Authorization: Bearer {token}`
- 支持 `TENANT-ID` 租户头
- **行业最佳实践**:
  - 心跳使用 JSON 格式 `{"type": "pong"}`，而非 comment 文本（便于客户端解析）
  - 连接建立时验证 token 有效性，无效拒绝连接
  - 支持 CORS 跨域配置（允许前端页面的域名）
  - 空闲超时自动断开（服务端 5 分钟无活动断开）

**Patterns to follow:**
- Spring WebFlux SSE 或 Spring MVC SseEmitter
- 连接建立时从 HttpServletRequest 提取 header 验证

**租户隔离:**
- 通过 `TENANT-ID` header 识别租户 (约定见 frontend-backend-chat.md §3)
- 连接上下文存储 tenant_id，断开时清理

**接口契约:**
- 连接: `GET /admin/sse/connect` + headers `Authorization`, `TENANT-ID`
- 消息: `data: {"type": "notification"|"alert", "content": "..."}`
- 心跳: 客户端30s发 `{"type": "ping"}`，服务端回复 `{"type": "pong"}`
- 约定见 frontend-backend-chat.md §3

**Test scenarios:**
- SSE 连接建立成功
- 断线自动重连 (最多 6 次，超过后提示用户)
- token 无效时连接被拒绝 (返回 401)

**Verification:**
- 前端 SSE 组件能接收消息

---

- [ ] **Unit A4: WebSocket 接口**

**Goal:** 实现 WebSocket 实时通信

**Dependencies:** 无

**Files:**
- Create: `mango/mango-websocket/` → WebSocket 模块
  - `mango-websocket-core/` → WebSocketHandler、心跳机制

**Approach:**
- 使用 Spring WebSocket
- **行业最佳实践**:
  - Token 在 WebSocket 握手阶段通过请求头传递（`Authorization` header），而非 URL query 参数（避免日志泄露、浏览器历史记录）
  - 握手时通过 `Sec-WebSocket-Protocol` 头传递 tenant_id，或通过第一个业务消息携带
  - 心跳使用 JSON 格式 `{"type": "pong"}`，而非 text 帧
  - Spring WebSocket `WebSocketInterceptor` 拦截握手阶段，验证 token 有效性
  - 空闲超时自动断开（服务端 5 分钟无活动断开）

**Patterns to follow:**
- Spring WebSocket `@EnableWebSocket` + `WebSocketConfigurer`
- Spring Security OAuth2 资源服务器验证 JWT token
- 握手拦截器 `HttpSessionHandshakeInterceptor` 或自定义 `WebSocketInterceptor`

**租户隔离:**
- 通过 `Authorization` header 提取 token，解析出 tenant_id
- 通过 `Sec-WebSocket-Protocol` 头传递 tenant_id（握手阶段）
- **安全校验**: 握手拦截器必须验证 `Sec-WebSocket-Protocol` header 与 token 解析出的 tenant_id 一致，不一致则拒绝连接
- 连接上下文存储 tenant_id，断开时清理

**接口契约:**
- 连接: `ws://host/admin/ws/chat` + header `Authorization: Bearer {token}` + `Sec-WebSocket-Protocol: {tenantId}`
- 客户端发: `{"type": "ping"}` 或 `{"type": "message", "content": "..."}`
- 服务端推: `{"type": "pong"}` 或 `{"type": "message", "content": "..."}`
- 心跳: 30s interval ping/pong
- 约定见 frontend-backend-chat.md §4

**Test scenarios:**
- WebSocket 握手时 token 无效返回 401 (连接拒绝)
- 握手成功建立连接
- 心跳维持
- 断线重连
- 多租户隔离验证

**Verification:**
- 前端 WebSocket 组件能收发消息
- token 无效时连接被拒绝

---

- [ ] **Unit A5: AI 对话接口**

**Goal:** 实现 AI 对话流式响应

**Dependencies:** 无

**Files:**
- Create: `mango/mango-ai/` → AI 模块
  - `mango-ai-api/` → API 层（接口定义、DTO）
  - `mango-ai-core/` → 核心业务（ChatService、DeepSeekProvider）
  - `mango-ai-starter/` → Starter 自动配置

**路径策略:** 同 Unit A1，由后端 Agent 选择独立服务模式或 BFF 代理模式

**简化 AI Provider (P2 MVP):**
- P2 首批只实现 DeepSeek Provider，不做完整 SPI 注册机制
- 后续扩展可按需升级为完整 SPI 架构
- 思维链: 后端透传 SSE 流，前端负责渲染

**输入验证 (安全防护):**
- 消息长度限制: 最大 2000 字符，超出返回错误
- 空消息校验: message 为空或空白字符返回错误
- 危险模式检测: 检测 Prompt Injection 模式，记录审计日志

**enableThinking 默认行为:**
- 前端未传 `enableThinking` 或传 `undefined` → 后端按 `true` 处理
- 显式传 `false` → 跳过 thinking 事件

**可配置存储策略 (P2 简化):**
- P2 只实现 **In-Memory** 存储 (ConcurrentHashMap + TTL)
- Session 淘汰: TTL (默认 30 分钟无访问淘汰)
- 存储键: `{sessionId + tenant_id}` 复合键隔离
- Redis/DB 降级放 P3 迭代

**接口契约:**
- 请求: `POST /admin/ai/chat` + headers `Authorization: Bearer {token}`, `TENANT-ID: {tenantId}`
- **TENANT-ID 校验**: 必须验证 `TENANT-ID` header 与 token 解析出的 tenant_id 一致，不一致返回 401
- 请求体: `{message, sessionId?, enableThinking?}` (enableThinking 未传/undefined → true)
- 响应 (SSE):
  - `data: {"type": "thinking", "content": "..."}`
  - `data: {"type": "message", "content": "..."}`
  - `data: {"type": "done", "sessionId": "..."}`
  - `data: {"type": "error", "message": "..."}`
- 约定见 frontend-backend-chat.md §5

**Patterns to follow:**
- ChatGPT SSE 流式响应格式

**租户隔离:**
- 通过 `TENANT-ID` header 识别租户
- ChatContext 按 `sessionId + tenant_id` 复合键隔离存储

**Test scenarios:**
- 发送消息返回流式响应
- 思维链正确分段（thinking → message → done）
- enableThinking=true 时返回思维链，enableThinking=false 时跳过 thinking 事件
- enableThinking 未传或 undefined 时按 true 处理
- 消息超过 2000 字符返回错误
- 空消息返回错误
- Session TTL 到期后上下文被清除
- 多租户隔离：租户 A 无法访问租户 B 的会话
- TENANT-ID header 与 token 不一致时返回 401

**Verification:**
- 前端 Chat 组件能显示 AI 回复
- 思维链可折叠/展开
- 流式响应逐字显示正常

---

### Phase B: 前端组件 (B1/B2无依赖可先行；B3-B7依赖后端 Unit A1-A5)

---

- [x] **Unit B1: useTitle 页面标题** ✓

**Goal:** 实现页面标题 i18n 支持

**Dependencies:** 无 (纯前端)

**Files:**
- Create: `mango-web/src/hooks/useTitle.ts`
- Create: `mango-web/src/hooks/__tests__/useTitle.spec.ts`

**Approach:**
- 自动拼接"页面标题 + 应用名称"
- 监听语言变化自动更新

**Patterns to follow:**
- `vue-i18n` useI18n 模式

**Test scenarios:**
- 切换语言后标题自动更新
- 无 i18n key 时只显示应用名

**Verification:**
- 浏览器标签页标题正确显示

---

- [x] **Unit B2: Sign 签名组件** ✓

**Goal:** 实现 Canvas 手写签名

**Dependencies:** 无 (纯前端)

**Files:**
- Create: `mango-web/src/components/Sign/` → index.vue, types.ts, i18n/
- Create: `mango-web/src/components/Sign/__tests__/Sign.spec.ts`
- Create: `mango-web/src/components/Sign/i18n/` → zh-cn.ts, en.ts

**Approach:**
- Canvas 绑定鼠标/触摸事件
- 支持线条颜色选择
- 输出 base64 签名图片

**交互状态:**
- **空状态**: Canvas 显示占位提示文字（如"请在此处签名"）
- **绘制状态**: 鼠标/触摸按下开始绘制
- **已签名状态**: 已有签名内容，显示清除按钮
- **清除操作**: 按钮重置 Canvas
- **错误状态**: base64 生成失败时显示错误提示

**Patterns to follow:**
- 现有组件结构 (el-button, el-dialog 模式)

**Test scenarios:**
- 鼠标绘制签名正常
- 触摸屏绘制正常
- 生成 base64 正常
- 空 Canvas 显示占位提示
- 清除按钮重置 Canvas
- base64 生成失败显示错误提示

**Verification:**
- v-model 正确绑定
- 空状态显示占位提示
- 清除操作可重置签名

---

- [ ] **Unit B3: ChinaArea 行政区选择器**

**Goal:** 实现省市区街道四级联动

**Dependencies:** Unit A1 (后端行政区划接口)

**Files:**
- Create: `mango-web/src/components/ChinaArea/` → index.vue, types.ts, i18n/
- Create: `mango-web/src/api/admin/area.ts`
- Create: `mango-web/src/components/ChinaArea/__tests__/ChinaArea.spec.ts`

**Approach:**
- el-cascader 封装
- 调用 `/admin/sysArea/tree` 获取数据
- 支持 type 参数
- 后端按需返回子节点，前端按层级展开

**交互状态:**
- **加载中**: 获取数据时显示加载指示器
- **错误状态**: API 请求失败时显示错误信息
- **空状态**: 节点无子级时显示"无数据"
- **正常状态**: 级联选择正常工作

**Patterns to follow:**
- 现有 API 调用模式 (get)

**Test scenarios:**
- 级联选择省市区正常
- 筛选功能正常
- v-model 双向绑定正常
- 加载中状态显示 spinner
- API 失败时显示错误提示
- 无数据节点显示"无数据"

**Verification:**
- 选择后 modelValue 返回 adcode 列表 (如 "440305")
- 切换语言后组件文本正确切换
- API 失败时有明确的错误反馈

---

- [ ] **Unit B4: OrgSelector 组织选择器**

**Goal:** 实现组织架构树形选择

**Dependencies:** Unit A2 (后端组织接口)

**Files:**
- Create: `mango-web/src/components/OrgSelector/` → index.vue, types.ts, i18n/
- Create: `mango-web/src/api/admin/org.ts`
- Create: `mango-web/src/components/OrgSelector/__tests__/OrgSelector.spec.ts`

**Approach:**
- el-tree 封装在 el-dialog 弹窗内
- 点击触发按钮打开弹窗，内部显示 el-tree
- 调用 `/admin/sysOrg/tree` 获取数据
- 支持多选，选中结果以 tag 形式回显
- **tree行为约定**: `check-strictly=false` (子节点选中不影响父节点)，emit 值为完整选中节点 ID 数组
- 前端自行处理半选状态，不依赖后端返回

**交互状态:**
- **加载中**: 获取数据时显示骨架屏或 spinner
- **错误状态**: API 请求失败时在弹窗内显示错误信息
- **空状态**: 租户无组织数据时显示"暂无可选组织"
- **正常状态**: 树形选择正常工作

**Patterns to follow:**
- 现有组件 el-dialog + el-tree 组合模式

**Test scenarios:**
- 勾选组织后正确回显
- 删除已选组织正常
- 加载中显示骨架屏
- API 失败时显示错误消息
- 空数据时显示"暂无可选组织"

**Verification:**
- 确定后 emit 值正确 (如 [1, 2, 3] 组织ID数组)
- 切换语言后组件文本正确切换
- API 失败时有明确的错误反馈

---

- [ ] **Unit B5: SSE 组件**

**Goal:** 实现 Server-Sent Events 实时通知

**Dependencies:** Unit A3 (后端 SSE 接口)

**Files:**
- Create: `mango-web/src/components/SSE/` → index.vue
- Create: `mango-web/src/components/SSE/__tests__/SSE.spec.ts`

**Approach:**
- 使用 `@microsoft/fetch-event-source`
- 支持重连机制
- 环境变量 VITE_SSE_ENABLE 控制

**Patterns to follow:**
- 现有通知模式 (ElNotification)

**Test scenarios:**
- 连接建立成功
- 收到消息弹出通知
- 断线重连正常
- 连接中显示"正在连接..."指示
- 重试中显示重试计数 (如"连接断开，正在重连 3/6")
- 重试耗尽后显示永久错误和手动重连按钮

**交互状态:**
- **连接中**: 显示"正在连接..."指示
- **重试中**: 显示重试计数 (如"连接断开，正在重连 3/6")
- **耗尽状态**: 6 次重试失败后，显示永久错误和手动重连按钮
- **已连接**: 指示变为绿色或隐藏

**Verification:**
- 打开页面时 SSE 连接建立 (Network 面板可见 text/event-stream 请求)
- 触发测试消息后 ElNotification 弹出通知
- 网络断开后自动重连 (最多 6 次，超过后提示用户)
- 重试过程中显示中间状态

---

- [ ] **Unit B6: WebSocket 组件**

**Goal:** 实现 WebSocket 实时通信

**Dependencies:** Unit A4 (后端 WebSocket 接口)

**Files:**
- Create: `mango-web/src/components/Websocket/` → index.vue
- Create: `mango-web/src/components/Websocket/__tests__/Websocket.spec.ts`

**Approach:**
- 原生 WebSocket 封装
- 心跳检测 (30s interval)
- 自动重连 (最多 6 次)

**Patterns to follow:**
- 现有组件生命周期模式

**Test scenarios:**
- 连接成功建立
- 心跳 ping/pong 正常
- 断线自动重连
- 连接中显示"正在连接..."指示
- 重试中显示重试计数
- 重试耗尽后显示永久错误和手动重连按钮

**交互状态:**
- **连接中**: 显示"正在连接..."指示
- **重试中**: 显示重试计数
- **耗尽状态**: 6 次重试失败后，显示永久错误和手动重连按钮
- **已连接**: 指示变为绿色或隐藏
- **断开期间**: 考虑未读消息计数显示

**Verification:**
- 断网后自动重连
- 重试过程中显示中间状态

---

- [ ] **Unit B7: Chat AI 对话组件**

**Goal:** 实现 AI 对话聊天

**Dependencies:** Unit A5 (后端 AI 接口)

> 注: Chat 组件使用 `/admin/ai/chat` 返回的独立 SSE 流，不依赖 B5 (SSE 通用通知组件)。

**Files:**
- Create: `mango-web/src/components/Chat/` → index.vue, types.ts, i18n/
- Create: `mango-web/src/components/Chat/__tests__/Chat.spec.ts`

**Approach:**
- 聊天窗口浮窗
- 显示思维链 (可折叠)
- 流式响应展示

**交互状态:**
- **初始空状态**: 显示欢迎语或推荐问题提示
- **流式状态**: AI 响应时显示光标闪烁或"AI 正在思考..."指示
- **思维链状态**: 单独可折叠区域，标注"思考中..."
- **错误状态**: API 请求失败时显示错误消息和重试按钮
- **完成状态**: 消息展示完整，停止光标动画

**浮窗交互:**
- 默认可见性: 页面加载时隐藏，点击-launcher 按钮打开
- 打开触发: 点击-launcher 按钮
- 收起行为: 最小化到右下角或完全关闭
- 位置: 右下角固定
- 不允许多实例

**SSE 事件处理:**
- `type: thinking` → 追加到思维链区域 (可折叠)
- `type: message` → 追加到对话区域
- `type: done` → 结束流，关闭思维链

**Patterns to follow:**
- 现有组件 el-input 模式

**Test scenarios:**
- 打开/关闭窗口正常
- 发送消息收到 AI 响应
- 思维链展开/折叠正常
- 流式响应逐字显示正常
- 欢迎语在空会话时显示
- 错误状态显示错误消息和重试按钮

**Verification:**
- 发送消息能收到回复
- 初始空状态显示欢迎提示
- 错误状态可重试

---

## 执行顺序

```
阶段 1: 先行单元 (无后端依赖，可与阶段 2 并行)
├── Unit B1: useTitle
└── Unit B2: Sign

阶段 2: 后端接口 (无依赖，可先行完成)
├── Unit A1: /admin/sysArea/tree
├── Unit A2: /admin/sysOrg/tree
├── Unit A3: /admin/sse/connect
├── Unit A4: /admin/ws/chat
└── Unit A5: /admin/ai/chat

阶段 3: 前端组件 (依赖阶段 2 后端接口)
├── Unit B3: ChinaArea (依赖 A1)
├── Unit B4: OrgSelector (依赖 A2)
├── Unit B5: SSE (依赖 A3)
├── Unit B6: WebSocket (依赖 A4)
└── Unit B7: Chat (依赖 A5, 可选 B5)
```

## AI 协作指南

| Agent | 负责单元 | 输入 | 输出 |
|-------|----------|------|------|
| 🍋 前端 Agent | Unit B1-B7 | 本计划 + frontend-backend-chat.md | 完整前端组件代码 |
| ☕ 后端 Agent | Unit A1-A5 | 本计划 + frontend-backend-chat.md | 完整后端模块代码 |

**并行开发:**
- 后端 Agent 可先实现 Unit A1-A5
- 前端 Agent 可先实现 Unit B1-B2
- 前后端 Agent 完成后，协同验证接口契约

## 前后端接口契约 (解耦关键)

> 以下契约是前后端唯一联调依据，双方独立开发，以契约为准。详细约定见 [frontend-backend-chat.md](../docs/frontend-backend-chat.md)

### 契约 A1: 行政区划树

| 字段 | 值 |
|------|-----|
| 路径 | `GET /mango/area/tree` (独立服务) 或 `GET /bff/admin/area/tree` (BFF代理) |
| Query | `type?: 1\|2\|3\|4`, `parentId?: number` |
| 响应 | `{code, msg, data: [{adcode, name, level, hot, children[]}]}` |
| 懒加载 | 按需展开，省→市→区/县→街道，每级返回全部子节点 |

### 契约 A2: 组织架构树

| 字段 | 值 |
|------|-----|
| 路径 | `GET /mango/org/tree` (独立服务) 或 `GET /bff/admin/org/tree` (BFF代理) |
| Query | `parentId?: number` (默认0查根节点) |
| 响应 | `{code, msg, data: [{id, name, parentId, sort, children[]}]}` |
| 懒加载 | 根节点+第一层子节点；展开按需加载 |
| 租户隔离 | tenant_id 字段隔离 |

### 契约 A3: SSE 推送

| 字段 | 值 |
|------|-----|
| 路径 | `GET /mango/sse/connect` (独立服务) 或 `GET /bff/admin/sse/connect` (BFF代理) |
| Headers | `Authorization: Bearer {token}`, `TENANT-ID: {tenantId}` |
| 推送格式 | `data: {"type": "notification"\|"alert", "content": "..."}` |
| 心跳 | 客户端30s发 `{"type": "ping"}`，服务端回 `{"type": "pong"}` (行业最佳实践 JSON 格式) |

### 契约 A4: WebSocket

| 字段 | 值 |
|------|-----|
| 路径 | `ws://host/mango/ws/chat` (独立服务) 或 `ws://host/bff/admin/ws/chat` (BFF代理) |
| Headers (握手) | `Authorization: Bearer {token}` (行业最佳实践，避免 URL 日志泄露) |
| Protocol | `Sec-WebSocket-Protocol: {tenantId}` |
| **安全校验** | `Sec-WebSocket-Protocol` header 必须与 token 解析出的 tenant_id 一致，否则拒绝连接 |
| 客户端发 | `{"type": "ping"}` 或 `{"type": "message", "content": "..."}` |
| 服务端推 | `{"type": "pong"}` 或 `{"type": "message", "content": "..."}` |
| 心跳 | 30s interval ping/pong (JSON 格式) |

### 契约 A5: AI 对话

| 字段 | 值 |
|------|-----|
| 路径 | `POST /mango/ai/chat` (独立服务) 或 `POST /bff/admin/ai/chat` (BFF代理) |
| Headers | `Authorization: Bearer {token}`, `TENANT-ID: {tenantId}` |
| **安全校验** | `TENANT-ID` header 必须与 token 解析出的 tenant_id 一致，否则返回 401 |
| **输入校验** | message 必填，最大 2000 字符；超过返回 error 事件 |
| 请求体 | `{message, sessionId?, enableThinking?}` (enableThinking 未传/undefined → true) |
| SSE响应 | `data: {"type": "thinking", "content": "..."}` → `data: {"type": "message", "content": "..."}` → `data: {"type": "done", "sessionId: "..."}` |
| AI服务商 | P2 首批 DeepSeek Provider (P3 扩展 SPI) |

### 前端组件与后端接口映射

| 前端组件 | 后端接口 | 前端开发依据 |
|----------|----------|-------------|
| ChinaArea (B3) | A1 | 契约A1 + el-cascader (省市区街道四级联动) |
| OrgSelector (B4) | A2 | 契约A2 + el-tree (check-strictly=false) |
| SSE (B5) | A3 | 契约A3 + fetch-event-source (受 VITE_SSE_ENABLE 控制，见环境变量章节) |
| WebSocket (B6) | A4 | 契约A4 + 原生WebSocket (受 VITE_WS_ENABLE 控制，见环境变量章节) |
| Chat (B7) | A5 | 契约A5 + fetch-event-source (独立 SSE 流，不依赖 B5) |

> **前端调用路径**: 统一使用 `/mango/*` 路径（后端 Agent 负责确保路由可达）

### AI Provider (P2 简化实现)

```
DeepSeekAiProvider (首批实现)
└── 后续扩展: GLMProvider, MiniMaxProvider, OpenAiProvider, ClaudeProvider (P3)

配置示例:
ai.provider=deepseek
ai.deepseek.base-url=https://api.deepseek.com
ai.deepseek.api-key=${DEEPSEEK_API_KEY}
ai.deepseek.model=deepseek-chat
```

> **SPI 说明**: P2 首批只实现 DeepSeek Provider，完整 SPI 机制放 P3 迭代

## 环境变量

新增前端环境变量:

```bash
# .env.development
VITE_SSE_ENABLE=false      # 是否启用 SSE
VITE_WS_ENABLE=false       # 是否启用 WebSocket
```

## System-Wide Impact

- **AI 组件定位**: Chat 用于 AI 对话场景 (智能客服、AI 助手)，非通用聊天
- **可选组件**: SSE/WebSocket 受环境变量控制，默认关闭
- **纯前端组件**: useTitle/Sign 为纯前端组件，不依赖后端
- **数据依赖组件**: ChinaArea/OrgSelector 依赖后端接口获取数据 (非无状态)

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| 前后端接口不匹配 | 严格按 frontend-backend-chat.md 约定接口 |
| AI 对话响应延迟 | 前端显示 loading 状态，超时提示 |
| WebSocket 断线 | 最多重连 6 次，之后提示用户 |
| 路径前缀选择错误 | 后端 Agent 根据 mango 模块定位选择（独立服务 vs BFF代理） |
| Prompt Injection 攻击 | 输入长度限制 + 危险模式检测 |
| 多租户数据泄露 | TENANT-ID header 与 token 解析值一致性校验 |

## Deferred to Implementation

以下问题已在计划中确认（无需实现阶段再决策）:

| 问题 | 决策 | 依据 |
|------|------|------|
| AI服务商具体选择 | **首批 DeepSeek Provider，P3 再扩展 SPI** | YAGNI 原则，P2 保持简单 |
| AI 对话上下文存储 | **P2 只做 In-Memory** | 简化复杂度，Redis/DB 降级放 P3 |
| enableThinking 默认值 | **undefined → true** | 向后兼容，前端未传按启用处理 |
| SSE 心跳格式 | **JSON pong** `{"type": "pong"}` | 行业最佳实践，便于客户端解析 |
| WebSocket token 安全 | **Handshake 阶段 header 传递 + Sec-WebSocket-Protocol 校验** | 行业最佳实践，Sec-WebSocket-Protocol 必须与 token 解析值一致 |
| mango-org 表数据复用方式 | 后端 Agent | 直接 SQL 查询，不引入 mango-org 依赖 |
| data.sqlite 导入 | **不使用 data.sqlite** | 直接使用 `mango/mango-area/sql/V1__mango_area_init.sql` |
| 路径前缀策略 | **独立服务 `/mango/*` 或 BFF `/bff/admin/*`** | 由后端 Agent 根据 mango 模块定位选择 |
| AI 输入验证 | **长度限制(2000字符) + 危险模式检测** | 安全防护，防止 Prompt Injection |
| TENANT-ID 校验 | **必须与 token 解析值一致** | 多租户安全红线，不一致返回 401 |
| OrgSelector 触发按钮 | 前端 Agent | 按钮 label 和 dialog title 由前端决定 |

以下问题在实现阶段由负责 Agent 自行决定，无预设约束。

## Sources & References

- **P2 组件计划:** [docs/plans/2026-03-31-001-feat-mango-web-p2-components-plan.md](../docs/plans/2026-03-31-001-feat-mango-web-p2-components-plan.md)
- **接口约定:** [docs/frontend-backend-chat.md](../frontend-backend-chat.md)
- **pigx-ui 源码:** `/Users/hardy/Work/pigx/pigx-ui`
