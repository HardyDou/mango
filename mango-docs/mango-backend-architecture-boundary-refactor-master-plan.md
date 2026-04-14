# Mango 后端架构边界收敛总文档

- 创建日期：2026-04-14
- 状态：进行中
- 类型：总目标文档

---

## 1. 文档定位

本文件不是 Sprint，不直接作为开发交付物验收。

本文件用于回答三个问题：

1. Mango 后端为了达成最终目标，当前还存在哪些架构缺陷
2. 后端重构的总目标是什么
3. 后续应拆成哪些可独立交付的子 Sprint

---

## 2. 最终目标

Mango 后端的最终目标不是“目录看起来分层”，而是形成一套真正适合 AI Agent 持续开发的后端底座。

目标状态如下：

- 采用 **模块化单体优先、远程化兼容保留** 的架构
- 通过 `api/core/starter/starter-remote` 提供稳定模块边界
- `starter` 只负责装配，不承载长期业务语义
- `app` 负责部署装配与运行配置
- `common` 只保留稳定、低耦合、跨模块公共契约
- `infra` 只提供技术能力，不依赖平台业务模型
- `platform` 模块按 bounded context 拆分，不再形成“平台杂项大模块”
- 文档、POM、目录、代码中的领域命名保持一致

---

## 3. 当前主要问题

### 3.1 公共层过重

`mango-common` 当前同时承载：

- 公共异常
- 统一返回模型
- 分页模型
- 校验注解
- JSON 工具
- Base64 工具
- TokenContext 兼容类
- 日志语义注解

风险：

- 成为全局耦合源
- 新增代码天然往 `common` 里堆
- AI Agent 无法稳定判断“什么该放 common、什么不该放”

### 3.2 基础设施层反向感知业务层

示例：

- `mango-infra-web` 依赖 `mango-rbac-api`

风险：

- infra 不再是通用底座
- 任何平台业务变动都可能反向污染基础设施层

### 3.3 平台重模块职责膨胀

重点模块：

- `mango-rbac`
- `mango-system`
- `mango-auth`

风险：

- `mango-rbac` 演变为用户中心 + 权限中心 + 菜单中心
- `mango-system` 演变为配置/租户/字典/日志/路由的杂项箱
- `mango-auth` 同时承担认证与安全治理

### 3.4 领域事实源不一致

表现：

- 文档中仍有 `mango-permission`
- POM 中仍有 `mango-user-*`
- 实际目录中却是另一套结构

风险：

- AI 生成代码时会错误引用模块
- Review 与实现基线不一致

---

## 4. 总体收敛原则

### 4.1 分层依赖

```text
app -> starter/adapter -> core -> api -> common-kernel
```

### 4.2 职责边界

- `common`: 公共内核与契约
- `infra`: 技术能力
- `platform`: 通用业务域
- `app`: 部署装配

### 4.3 重构顺序

按照“错误依赖扩散源头优先”的顺序推进：

1. `mango-common`
2. `mango-infra-web` / `mango-infra-security`
3. `mango-infra-kv`
4. `mango-rbac`
5. `mango-system`
6. `mango-auth`
7. `mango-admin-app`

---

## 5. 子 Sprint 路线图

### Sprint 09

文档：`mango-docs/plans/2026-04-14-sprint-09-mango-common-kernel-contract-refactor.md`

计划起始日期：2026-04-14

目标：

- 将 `mango-common` 收敛为公共内核与公共契约
- 清理空基类、兼容类、技术工具类的错误归属

### Sprint 10

文档：`mango-docs/plans/2026-04-14-sprint-10-infra-web-security-boundary-decoupling.md`

计划起始日期：2026-04-15

目标：

- 去除 `infra` 层对平台业务模型的反向依赖
- 收口 Web、安全、上下文三类基础能力的边界

### Sprint 11

文档：`mango-docs/plans/2026-04-14-sprint-11-platform-rbac-system-boundary-phase1.md`

计划起始日期：2026-04-16

目标：

- 收敛 `mango-rbac` 与 `mango-system` 的职责边界
- 明确 identity / authorization / config / tenant / audit / dict 的域边界

### Sprint 12

文档：`mango-docs/plans/2026-04-14-sprint-12-auth-admin-app-boundary-assembly-cleanup.md`

计划起始日期：2026-04-17

目标：

- 收敛 `mango-auth` 的认证职责
- 清理 `mango-admin-app` 的装配层职责与错误依赖

---

## 6. 各 Sprint 之间的依赖关系

```text
Sprint 09 -> Sprint 10 -> Sprint 11 -> Sprint 12
```

说明：

- Sprint 09 完成后，后续模块有稳定公共依赖基础
- Sprint 10 完成后，平台层不再被 infra 反向耦合
- Sprint 11 完成后，重型平台模块边界收敛
- Sprint 12 完成后，认证与装配层完成收口

---

## 7. 总体验收口径

总文档本身不验收代码，验收口径体现在各子 Sprint 中。

总文档完成的标志是：

- 总目标明确
- 重构顺序明确
- 子 Sprint 边界明确
- 每个 Sprint 都能独立交付
