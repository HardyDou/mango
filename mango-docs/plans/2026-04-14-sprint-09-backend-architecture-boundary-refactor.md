# Sprint 09: 后端架构边界收敛与 `mango-common` 内核重构

- 起始日期：2026-04-14
- 状态：待执行
- 所属任务：T9
- 关联 plan：`2026-04-14-sprint-08-backend-optimization-plan.md`

---

## 1. 背景与目标

当前 `mango` 后端已经形成了较完整的模块化骨架：

- 基础设施层：`mango-common`、`mango-infra-*`
- 平台能力层：`mango-auth`、`mango-rbac`、`mango-system`、`mango-org`、`mango-area` 等
- 应用装配层：`mango-app/mango-admin-app`

整体方向与项目最终目标一致，即：

1. 面向 AI Agent 的可执行开发底座
2. 基于 `SPI + starter` 的部署形态切换
3. 通过清晰分层和稳定契约降低 AI 生成代码的不确定性
4. 通过统一规范、统一模块边界、统一基础能力注入，提升可维护性和可扩展性

但经过本轮代码和架构审视，发现当前后端存在一类更深层的问题：

- **模块边界不稳定**：部分模块严格按 `api/core/starter/starter-remote` 拆分，部分模块则仍混合 controller、业务服务、装配逻辑。
- **公共层过重**：`mango-common` 同时承载公共内核、接口契约、Web 辅助、加密工具、兼容层，已成为潜在耦合源。
- **基础设施层反向耦合业务层**：例如 `mango-infra-web` 依赖 `mango-rbac-api`。
- **领域边界漂移**：根 POM 和 `mango-admin-app` 仍残留 `mango-user-*` 依赖，但仓库当前并无对应平台模块。
- **文档与实现漂移**：如 `mango-rbac` 文档仍以 `mango-permission` 命名，削弱“唯一事实源”。

本 Sprint 不追求一次性大改所有模块，而是先做一轮 **架构边界收敛**，将错误依赖和错误职责的扩散源头收住。

---

## 2. Sprint 核心目标

### Goal A: 明确后端架构的最终落点

将 `mango` 后端明确收敛为：

- **模块化单体优先**
- **远程化兼容保留**
- **starter 负责装配，不负责承载业务语义**
- **common 只保留稳定、低耦合、跨模块可复用的公共内核**

### Goal B: 完成 `mango-common` 的重构设计与第一阶段落地

把 `mango-common` 从“公共杂物箱”收敛成“公共内核 + 接口契约”，为后续 `infra` 与 `platform` 模块重构打基础。

### Goal C: 制定后续模块级重构路线

为 `mango-infra-*`、`mango-rbac`、`mango-system`、`mango-auth` 等重模块建立清晰的改造顺序和边界规则，避免后续继续无序演进。

---

## 3. 核心问题清单

### 3.1 领域命名与模块事实不一致

| 问题 | 位置 | 风险 |
|------|------|------|
| 残留 `mango-user-*` 依赖 | `mango/pom.xml`, `mango-admin-app/pom.xml` | AI 与开发者误判真实领域边界 |
| `mango-rbac` 文档仍使用 `mango-permission` | `mango-platform/mango-rbac/README.md` | 文档与实现脱节，污染架构认知 |

### 3.2 `mango-common` 职责过载

当前 `mango-common` 同时包含：

- 统一返回结果 `R`
- 业务异常 `BizException`
- 断言工具 `Require`
- `BasePO` / `BaseVO` / `PagePO` / `PageVO`
- 参数校验注解
- `JacksonUtils`
- `Base64Utils`
- 兼容型 `TokenContextHolder`
- `@Log` / `LogType`

这已经超出了“公共内核”的合理边界。

### 3.3 infra 与 platform 之间存在反向耦合

| 问题 | 位置 | 风险 |
|------|------|------|
| `mango-infra-web` 依赖 `mango-rbac-api` | `mango-infra-web/pom.xml` | 基础设施层感知业务权限模型 |
| `mango-infra-kv` 承担过多能力 | `mango-infra-kv-*` | 模块膨胀，后续变为新的“公共泥球” |

### 3.4 平台模块职责边界不清

| 模块 | 当前问题 | 风险 |
|------|----------|------|
| `mango-rbac` | 同时承载用户、角色、菜单、公共路径、权限校验 | 趋于演化为“用户中心 + 权限中心 + 菜单中心” |
| `mango-system` | 同时承载字典、租户、配置、日志、路由 | 成为平台杂项收纳箱 |
| `mango-auth` | 同时承载登录、验证码拦截、签名校验、反重放等 | 认证域与安全治理域混淆 |

---

## 4. 架构收敛原则

本 Sprint 完成后，后续所有模块演进必须遵循以下原则：

### 4.1 分层依赖规则

```text
app -> starter/adapter -> core -> api -> common-kernel
```

强制约束：

- `app` 负责装配与部署
- `starter` 负责自动配置和本地装配
- `core` 负责业务逻辑
- `api` 负责稳定接口契约
- `common` 只保留低耦合公共内核

### 4.2 starter 职责规则

`starter` 不再承担以下职责：

- 不新增复杂业务逻辑
- 不承载领域服务实现
- 不再作为长期 controller 落点

后续 HTTP 暴露建议逐步回收到：

- `app` 层，或
- 专门的 `adapter-web` 层

### 4.3 common 职责规则

可放入 `common` 的能力必须同时满足：

1. 跨模块复用
2. 与具体业务域无关
3. 与具体技术实现低耦合
4. 在 2-3 年内语义稳定

### 4.4 infra 职责规则

infra 只提供：

- 中间件抽象
- 通用 Web 能力
- 可观测性
- 安全原语
- 数据访问基础设施

infra 不允许依赖平台业务模型。

---

## 5. Phase 1: `mango-common` 内核重构设计

### 5.1 目标

将当前 `mango-common` 拆解为更清晰的职责层：

```text
mango-common-kernel
├── code
├── exception
├── assertx
└── pagination

mango-common-contract
├── response
├── request
└── validation

mango-common-legacy
└── compatibility bridge
```

> 注：本 Sprint 可以先完成包级重构设计和第一阶段迁移，不强制一次性拆出全部独立 Maven 模块；但目标结构必须确定。

### 5.2 `mango-common` 保留项

建议保留在新公共内核或契约层中的内容：

- `BizCode`
- `BizException`
- `Require`
- 分页请求 / 分页响应模型
- 校验注解（如 `@Phone`, `@IdCard`）
- 统一 API 响应模型 `R<T>` 或其等价替代

### 5.3 `mango-common` 迁出项

以下内容不再长期保留在 `mango-common`：

| 内容 | 当前类 | 目标位置 |
|------|--------|----------|
| Base64 工具 | `Base64Utils` | `mango-infra-crypto` |
| JSON 工具 | `JacksonUtils` | 删除或改为 Spring 统一 `ObjectMapper` |
| Token 上下文兼容类 | `TokenContextHolder` | `mango-infra-security` / `mango-infra-context` |
| 日志语义注解 | `@Log`, `LogType` | `mango-system-audit` 或独立审计 API |
| 空基类 | `BasePO`, `BaseVO` | 删除 |

### 5.4 `mango-common` 具体重构动作

1. **统一响应协议**
   - 评估 `R<T>` 的字段命名，收敛为单一返回协议
   - 避免 `msg/message/success` 多套语义长期共存

2. **移除异常副作用**
   - `BizException` 构造函数中不再直接打日志
   - 日志交给统一异常处理层

3. **重命名分页模型**
   - `PagePO` / `PageVO` 重构为更清晰的 `PageQuery` / `PageResult`

4. **删除空父类**
   - 清理 `BasePO`, `BaseVO`

5. **处理兼容层**
   - 旧包或旧类加 `@Deprecated`
   - 在迁移完成后删除

### 5.5 第一阶段验收结果

完成本阶段后，需达到：

- `mango-common` 不再依赖 Web / Servlet / AOP / Crypto 的实现性依赖
- `mango-common` 的包结构能够区分“公共内核”和“接口契约”
- 全仓库新代码不再新增对旧兼容类的引用

---

## 6. Phase 2: infra 边界收敛路线

### 6.1 `mango-infra-web`

#### 当前问题

- 承担全局异常处理和通用 filter 是合理的
- 但依赖 `mango-rbac-api`，说明基础设施层已感知业务权限模型

#### 改进建议

1. 移除对 `mango-rbac-api` 的直接依赖
2. 仅保留：
   - 全局异常处理
   - Web 基础配置
   - 通用 filter / internal call filter
3. 权限相关能力改为依赖抽象接口，而非平台模型

### 6.2 `mango-infra-security`

#### 当前问题

- 同时存在 token service、permission service、AOP 注解，方向是对的
- 但与 `mango-auth`、`mango-rbac` 的业务职责边界仍需继续切清

#### 改进建议

1. 明确保留能力：
   - `ITokenService`
   - `IPermissionService`
   - `@Perm` 注解与切面
2. 禁止继续放入登录业务、用户业务、权限同步业务
3. 接管通用 token context 能力

### 6.3 `mango-infra-kv`

#### 当前问题

`mango-infra-kv` 已承载：

- cache
- lock
- token store
- counter
- id generator
- idempotent
- rate limiter
- serializer / converter
- AOP aspect

已经明显超出单模块合理边界。

#### 改进建议

后续拆分方向：

```text
mango-infra-cache
mango-infra-lock
mango-infra-idempotency
mango-infra-rate-limit
mango-infra-token-store
```

本 Sprint 至少完成：

1. 模块拆分方案设计
2. 接口族分类
3. properties 分类
4. 后续迁移顺序确定

### 6.4 `mango-infra-db`

#### 改进建议

1. 明确它是数据库基础设施层，而不是业务持久化容器
2. 收敛到：
   - datasource
   - Flyway
   - MyBatis / ORM 配置
3. 不在该层注入业务语义

### 6.5 `mango-infra-feign`

#### 改进建议

补齐统一内部调用治理规范：

- trace id 透传
- tenant id 透传
- internal signature
- timeout / retry policy
- 错误码映射策略

---

## 7. Phase 3: platform 模块边界收敛路线

### 7.1 `mango-rbac`

#### 当前问题

同时承载：

- 用户
- 角色
- 菜单
- 公共路径
- 权限校验

#### 改进建议

后续拆解为两个 bounded context：

1. `identity`
   - 用户
   - 用户资料
   - 用户组织归属

2. `authorization`
   - 角色
   - 权限
   - 菜单
   - 公共路径

### 7.2 `mango-system`

#### 当前问题

同时承载：

- 字典
- 租户
- 配置
- 操作日志
- 登录日志
- 路由

#### 改进建议

后续拆解为：

- `mango-config`
- `mango-tenant`
- `mango-audit`
- `mango-dict`
- `mango-route-registry`

### 7.3 `mango-auth`

#### 当前问题

认证域和安全治理域混合，包括：

- 登录
- refresh token
- 登录限流
- 验证码拦截
- 签名校验
- 反重放

#### 改进建议

认证域聚焦为：

- 身份认证
- token 生命周期
- auth user provider

签名校验、反重放、幂等、防刷等下沉至安全治理层。

### 7.4 轻量平台模块

以下模块以“标准模板化”为目标，不做大拆：

- `mango-area`
- `mango-org`
- `mango-i18n`
- `mango-captcha`
- `mango-message`
- `mango-ai`

本 Sprint 对这些模块的要求：

1. 明确其 `api/core/starter/remote` 目标边界
2. 不再向 `starter` 新增复杂业务逻辑
3. 后续作为标准模块模板沉淀

---

## 8. Phase 4: `mango-admin-app` 与装配层规则

### 当前问题

`mango-admin-app` 当前承担了应用装配职责，但依赖集中仍存在领域漂移和基础设施泄漏问题。

### 改进建议

1. 将 `mango-admin-app` 明确为“唯一应用装配层”
2. 负责：
   - profile
   - starter 组合
   - 部署与运行配置
3. 不继续承载底层实现细节
4. 清理残留 `mango-user-*` 依赖，或补齐其真实领域定义

---

## 9. 任务拆解

### Task 1: 架构事实源清理

- [ ] 全局梳理 `mango-user-*` 残留引用
- [ ] 修正文档中 `mango-permission` / `mango-rbac` 漂移
- [ ] 更新架构事实文档，明确当前真实模块图

### Task 2: `mango-common` 重构设计与第一阶段迁移

- [ ] 制定 `mango-common-kernel` / `contract` / `legacy` 的目标结构
- [ ] 重构 `BizException`
- [ ] 统一 `R<T>` 协议
- [ ] 设计分页模型替代方案
- [ ] 删除 `BasePO` / `BaseVO`
- [ ] 迁出 `Base64Utils`
- [ ] 处理 `JacksonUtils`
- [ ] 处理废弃 `TokenContextHolder`

### Task 3: infra 边界收敛设计

- [ ] 设计 `mango-infra-web` 去业务依赖方案
- [ ] 设计 `mango-infra-security` 与 `mango-auth` 的边界规则
- [ ] 输出 `mango-infra-kv` 的拆分方案
- [ ] 补齐 `mango-infra-feign` 的治理清单

### Task 4: platform 边界收敛设计

- [ ] 输出 `mango-rbac` bounded context 拆分方案
- [ ] 输出 `mango-system` 拆分方案
- [ ] 输出 `mango-auth` 聚焦收敛方案
- [ ] 标记轻量平台模块的模板化约束

### Task 5: 装配层规则收敛

- [ ] 明确 `mango-admin-app` 的职责边界
- [ ] 识别应逐步迁离 `starter` 的 controller
- [ ] 建立后续 `adapter-web` / `app` 收口策略

---

## 10. 验收标准

### 10.1 架构文档验收

- [ ] 新的架构边界规则已写入 Sprint 文档并经评审确认
- [ ] `mango-common` 的目标结构已明确
- [ ] `infra` / `platform` / `app` 的职责边界已形成统一定义

### 10.2 代码结构验收

- [ ] `mango-common` 第一阶段重构已落地或至少已完成包级/模块级拆分设计
- [ ] 不再新增对旧兼容类的引用
- [ ] 已列出 `mango-infra-web`、`mango-infra-kv`、`mango-rbac`、`mango-system` 的明确后续改造入口

### 10.3 一致性验收

- [ ] 文档中的模块命名与仓库实际目录一致
- [ ] 根 POM / app POM 中的领域依赖与真实模块一致
- [ ] 不再出现“文档一套、目录一套、依赖一套”的情况

### 10.4 质量门禁

- [ ] 所有重构设计都能映射到清晰的模块职责
- [ ] 不引入新的基础设施层反向依赖业务层问题
- [ ] 后续 Sprint 可基于本计划继续拆分实施

---

## 11. 后续 Sprint 建议

本 Sprint 完成后，建议按以下顺序继续推进：

1. **Sprint 10**
   - `mango-common` 真正拆分落地
   - 统一异常与响应协议

2. **Sprint 11**
   - `mango-infra-web` / `mango-infra-security` / `mango-infra-kv` 边界重构

3. **Sprint 12**
   - `mango-rbac` 与 `mango-system` bounded context 拆分

4. **Sprint 13**
   - `mango-auth` 聚焦收敛
   - `mango-admin-app` 装配层收口

---

## 12. 预期收益

完成本 Sprint 后，将获得以下收益：

- 为 AI Agent 提供更清晰、更稳定的模块边界
- 降低 `common` 和重型平台模块的继续膨胀风险
- 为后续 `infra` / `platform` 重构提供统一入口
- 提升生成代码、Review 代码、自动检查代码时的可判定性
- 把项目从“目录看似模块化”推进到“依赖与职责真实模块化”
