# Sprint 15: 能力自动注册与 Remote Adapter 重构

- 起始日期：2026-04-17
- 状态：待执行
- 所属任务：T15
- 前置 Sprint：`Sprint 14`
- 关联规范：
  - `mango-pmo/rules/backend/03-api.md`
  - `mango-pmo/rules/backend/05-module.md`

---

## 1. 目标

建立 Mango 能力自动注册机制。

解决问题：

- `*-api` 中不再直接使用 `@FeignClient`
- `starter-remote` 不再硬编码服务发现名
- 聚合部署优先走本地实现
- 微服务部署通过能力解析找到远程服务
- 管理后台后续可查看和维护能力注册信息

---

## 2. 架构定位

- 能力注册属于 `mango-infra`。
- 业务模块只声明和注册自己提供的能力。
- 管理后台只做能力数据查看、配置和运维。
- 管理后台不实现能力注册核心逻辑。

---

## 3. 范围

### In Scope

- 新增能力注册抽象
- 支持进程内 `memory` 注册
- 支持配置文件 `config` 注册
- 调整 `starter` 自动注册能力
- 调整 `starter-remote` 通过能力解析远程目标
- 清理 `*-api` 中的 `@FeignClient`
- 清理硬编码服务发现名
- 增加程序检查候选规则

### Out of Scope

- 本 Sprint 不实现管理后台页面
- 本 Sprint 不实现 Redis / DB / Nacos 注册后端
- 本 Sprint 不自研 Raft / Gossip / 区块链式注册
- 本 Sprint 不重构全部业务功能
- 本 Sprint 不处理 `mango-auth` / `mango-admin-app` 职责收口

---

## 4. 执行前置

本 Sprint 必须在 `Sprint 14` 之后执行。

前置要求：

- `*-api` 禁止 `@FeignClient` 已进入规则候选
- `starter-remote` 禁止硬编码服务发现名已进入规则候选
- `starter` 对外能力必须注册已进入规则候选
- 自动检查落点已明确

---

## 5. 设计规则

### API 契约

- `*-api` 只定义 `XxxApi`。
- `*-api` 禁止 `@FeignClient`。
- `XxxApi` 不关心部署位置。

### 本地实现

- `starter` 实现 `XxxApi`。
- `starter` 启动时注册本模块能力。
- 同进程存在本地实现时优先使用本地实现。

### 远程实现

- `starter-remote` 提供 Feign adapter。
- Feign adapter 继承 `XxxApi`。
- 远程服务名通过能力解析获得。
- 不允许在代码中硬编码服务发现名。

### 能力注册

能力注册信息必须包含：

- `capability`
- `providerModule`
- `serviceName`
- `basePath`
- `mode`
- `version`

---

## 6. 建议模块

新增基础设施模块：

- `mango-infra-capability-api`
- `mango-infra-capability-core`
- `mango-infra-capability-starter`

后续可选模块：

- `mango-infra-capability-starter-nacos`
- `mango-infra-capability-starter-redis`
- `mango-infra-capability-starter-db`

---

## 7. 核心接口

```java
public interface MangoCapabilityRegistry {

    void register(CapabilityRegistration registration);

    Optional<CapabilityEndpoint> resolve(String capability);
}
```

```java
public record CapabilityRegistration(
        String capability,
        String providerModule,
        String serviceName,
        String basePath,
        String mode,
        String version) {
}
```

---

## 8. 第一批改造对象

- `mango-gateway-api` 的 `SysPublicPathApi`
- `mango-rbac-api` 的 `SysPublicPathApi`
- `mango-auth-starter-remote`
- `mango-rbac-starter-remote`
- `mango-org-starter-remote`
- `mango-area-starter-remote`
- `mango-i18n-starter-remote`

---

## 9. 实施步骤

### Task A: 能力注册抽象

- [ ] 新增 `mango-infra-capability`
- [ ] 定义 `MangoCapabilityRegistry`
- [ ] 定义 `CapabilityRegistration`
- [ ] 定义 `CapabilityEndpoint`

### Task B: Memory / Config 实现

- [ ] 实现进程内 memory registry
- [ ] 实现配置文件 registry
- [ ] 支持 `mango.capability.registry.type=auto`
- [ ] 支持本地优先、配置兜底

### Task C: Starter 自动注册

- [ ] 为首批模块增加能力注册逻辑
- [ ] 注册能力名、服务名、路径、部署模式
- [ ] 服务名默认读取 `spring.application.name`

### Task D: Remote Adapter 改造

- [ ] 从 `*-api` 移除 `@FeignClient`
- [ ] 在 `*-starter-remote` 增加 Feign adapter
- [ ] Feign 目标通过能力解析或配置得到
- [ ] 移除硬编码服务发现名

### Task E: 检查规则

- [ ] 新增规则：`*-api` 禁止 `@FeignClient`
- [ ] 新增规则：`starter-remote` 禁止硬编码服务发现名
- [ ] 新增规则：`starter` 必须注册对外能力

---

## 10. 验收标准

- [ ] `*-api` 不再包含 `@FeignClient`
- [ ] 首批 remote adapter 已迁移到 `starter-remote`
- [ ] 本地聚合部署可走本地实现
- [ ] 远程部署可通过能力解析找到目标服务
- [ ] `memory` registry 可用
- [ ] `config` registry 可用
- [ ] 首批检查规则已进入 Sprint 14 自动化候选清单
- [ ] focused `mvn test` 通过
- [ ] focused `mvn verify` 通过

---

## 11. 对后续 Sprint 的影响

- `Sprint 12` 必须在本 Sprint 完成后执行。
- `Sprint 12` 需要基于能力注册机制清理 auth / admin-app 装配关系。
- 后续管理后台只展示和维护能力注册数据。
- 后续管理后台不承载能力注册核心逻辑。

---

## 12. 后续计划

- 管理后台展示能力注册表
- 支持能力启停和覆盖配置
- 支持 Nacos registry
- 支持 Redis / DB registry
- 支持能力调用健康状态展示
