# Sprint 15: 模块信息自动采集与 Remote Adapter 重构

- 起始日期：2026-04-17
- 状态：已完成
- 所属任务：T15
- 前置 Sprint：`Sprint 14`
- 关联规范：
  - `mango-pmo/rules/backend/03-api.md`
  - `mango-pmo/rules/backend/05-module.md`

---

## 1. 目标

建立 Mango 模块信息自动采集机制。

解决问题：

- `*-api` 中不再直接使用 `@FeignClient`
- Feign 代码按模块名声明目标
- 聚合部署时可解析真实服务名和 contextPath
- 本地聚合部署优先走本地实现
- 管理后台后续可查看模块部署信息

---

## 2. 架构定位

- 模块信息属于 `mango-infra`。
- 模块名是调用方唯一稳定标识。
- `starter` 只提供 `META-INF/mango/module.properties`。
- 应用装配层必须依赖 `mango-infra-module-starter`。
- 管理后台只做模块信息查看、配置和运维。
- 管理后台不实现模块信息采集核心逻辑。

---

## 3. 范围

### In Scope

- 新增 `mango-infra-module`
- 支持进程内 `memory` 模块信息
- 支持配置文件覆盖模块信息
- `starter` 通过资源文件声明模块名
- 自动采集当前 `spring.application.name`
- 自动采集当前 contextPath
- 清理 `*-api` 中的 `@FeignClient`
- 清理硬编码真实服务名
- 增加程序检查候选规则

### Out of Scope

- 本 Sprint 不实现管理后台页面
- 本 Sprint 不实现 Redis / DB / Nacos 后端
- 本 Sprint 不自研 Raft / Gossip / 区块链式注册
- 本 Sprint 不重构全部业务功能
- 本 Sprint 不处理 `mango-auth` / `mango-admin-app` 职责收口

---

## 4. 设计规则

### API 契约

- `*-api` 只定义 `XxxApi`。
- `*-api` 禁止 `@FeignClient`。
- `XxxApi` 不关心部署位置。

### 本地实现

- `starter` 实现 `XxxApi`。
- `starter` 必须提供 `META-INF/mango/module.properties`。
- `module.properties` 必须声明 `module-name`。
- 应用启动时自动采集本地模块信息。
- 同进程存在本地实现时优先使用本地实现。

### 远程实现

- `starter-remote` 提供 Feign adapter。
- Feign adapter 继承 `XxxApi`。
- Feign `name` 保持目标模块名。
- 真实服务名和 contextPath 通过模块信息解析获得。
- 不允许在代码中硬编码真实服务名。
- 禁止新增 `mango.remote.*`。

### 模块信息

模块信息必须包含：

- `moduleName`
- `serviceName`
- `contextPath`
- `source`

配置前缀：

```yaml
mango:
  module:
    module-service:
      modules:
        mango-rbac:
          service-name: mango-admin-app
          context-path: /admin
```

---

## 5. 模块

新增基础设施模块：

- `mango-infra-module-api`
- `mango-infra-module-core`
- `mango-infra-module-starter`

后续可选模块：

- `mango-infra-module-nacos`
- `mango-infra-module-redis`
- `mango-infra-module-db`

---

## 6. 核心接口

```java
public interface ModuleInfoRegistry {

    void register(ModuleInfo moduleInfo);

    Optional<ModuleInfo> resolve(String moduleName);
}
```

```java
public record ModuleInfo(
        String moduleName,
        String serviceName,
        String contextPath,
        String source) {
}
```

---

## 7. 第一批改造对象

- `mango-gateway-api` 的 `SysPublicPathApi`
- `mango-rbac-api` 的 `SysPublicPathApi`
- `mango-rbac-starter`
- `mango-auth-starter`
- `mango-org-starter`
- `mango-area-starter`
- `mango-i18n-starter`

---

## 8. 实施步骤

### Task A: 模块信息抽象

- [x] 新增 `mango-infra-module`
- [x] 定义 `ModuleInfoRegistry`
- [x] 定义 `ModuleInfoResolver`
- [x] 定义 `ModuleInfo`

### Task B: Memory / Config 实现

- [x] 实现进程内 memory registry
- [x] 支持配置文件覆盖
- [x] 支持 `mango.module.module-service`
- [x] 明确配置覆盖优先级：配置覆盖自动采集结果

### Task C: Starter 自动采集

- [x] 定义 `META-INF/mango/module.properties`
- [x] 采集模块名、服务名、contextPath
- [x] 服务名默认读取 `spring.application.name`
- [x] 为首批 starter 补齐 `module.properties`

### Task D: Remote Adapter 改造

- [x] 从 `*-api` 移除 `@FeignClient`
- [x] 在 `*-starter-remote` 增加或调整 Feign adapter
- [x] Feign 目标通过模块信息解析得到
- [x] 移除硬编码真实服务名

### Task E: 检查规则

- [x] 新增规则：`*-api` 禁止 `@FeignClient`
- [x] 新增规则：`starter-remote` 禁止硬编码真实服务名
- [x] 新增规则：`starter` 必须提供 `META-INF/mango/module.properties`

---

## 9. 验收标准

- [x] `*-api` 不再包含 `@FeignClient`
- [x] 首批 starter 已提供 `META-INF/mango/module.properties`
- [x] 本地聚合部署可自动采集模块信息
- [x] 远程部署可通过模块信息解析目标服务
- [x] `memory` registry 可用
- [x] `config` 覆盖可用
- [x] 首批检查规则已进入 `mango:check`
- [x] focused `mvn test` 通过
- [x] focused `mvn verify` 通过

---

## 10. 对后续 Sprint 的影响

- `Sprint 12` 必须在本 Sprint 完成后执行。
- `Sprint 12` 需要基于模块信息机制清理 auth / admin-app 装配关系。
- 后续管理后台只展示和维护模块信息。
- 后续管理后台不承载模块信息采集核心逻辑。

---

## 11. 后续执行跟踪

### 2026-04-17 后端模块级重构计划 Review

| 跟踪项 | 结论 | 证据 |
|--------|------|------|
| 模块级重构计划纳入后续执行 | 已纳入 | `./2026-04-17-backend-module-by-module-refactor-plan.md` 作为后端后续 Phase 执行主入口 |
| Phase 0 review 结果 | 已补齐 | `./2026-04-17-phase-0-fact-source-delivery-record.md` |
| `mango-user` 残留验证 | 已分类 | 当前有效源码/POM 无 `mango-rbac` 或根 POM 旧依赖；剩余命中为工具生成器示例/测试与 gateway 默认服务名 |
| `mango-rbac` 删除 `mango-user-api` 后编译 | 通过 | `mvn -q -pl mango-platform/mango-rbac -am -DskipTests compile` |
| gateway 旧服务名 | 后续处理 | `GatewayProperties.userService = "mango-user-starter"` 登记为 Phase 5 输入 |
| `mango-gateway-starter-remote` 依赖越界 | 后续处理 | 仍依赖 `mango-gateway-core`、`mango-infra-security-starter`；按 `starter-remote` 边界规则登记到 Phase 5 处理 |

执行规则：

- 后续后端重构按模块级 Phase 计划推进，不再以 Sprint 12 / Sprint 15 的历史顺序覆盖当前 Phase 顺序。
- Sprint 15 的模块信息机制作为后续 Phase 的能力输入；涉及 auth / admin-app / gateway 的边界调整，进入对应 Phase 后再实施。

---

## 11. 后续计划

- 管理后台展示模块部署信息
- 支持 Nacos 后端
- 支持 Redis / DB 后端
