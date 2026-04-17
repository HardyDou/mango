# Phase 1 交付记录

## 范围

- 本次处理模块：`mango-common`。
- 必要下游适配：`mango-ai-api` 补充自身使用的 `jakarta.validation-api` 依赖。
- 本次未处理模块：`mango-infra`、`mango-platform` 业务边界、`mango-app` 装配边界、gateway 服务名策略。

## 完成项

- [x] 已按 Phase 1 要求先盘点 `mango-common` 当前类清单。
- [x] 已输出 `mango-common` 类归属表：`mango-docs/plans/2026-04-17-phase-1-common-class-ownership.md`。
- [x] 已标记每个类归属：保留 / 删除，并说明引用模块与处理原因。
- [x] 已保留稳定公共契约：`BizException`、`BizCode`、`CommonCode`、`R`、`Require`、`PageQuery`、`PageResult`。
- [x] 已删除无外部引用且带地区/业务语义的校验类：`Phone`、`PhoneValidator`、`IdCard`、`IdCardValidator`。
- [x] 已从 `mango-common/pom.xml` 移除 `spring-boot-starter-validation`，避免公共内核承载 validation starter。
- [x] 已更新 `mango-common/README.md`，写入准入规则、禁止进入清单和 Phase 1 结论。
- [x] 已明确 `SysUser`、权限模型、组织模型、业务消息模型禁止进入 `mango-common`。
- [x] 已更新 `mango-docs/index.md`，补充 Phase 0 交付记录、Phase 1 类归属表和 Phase 1 交付记录入口。

## 主动不做项

- [x] 事项：不迁移或上移 `SysUser`、权限模型、组织模型、消息模型。
  原因：Phase -1 与 Phase 1 禁止将业务模型放入 `mango-common`。
- [x] 事项：不把 `Require` 迁出或删除。
  原因：当前 `Require` 只依赖公共错误契约并抛出 `BizException`，已有 `mango-org-core` 与生成器模板使用；本阶段将其限定为轻量契约断言。
- [x] 事项：不为手机号、身份证号校验新增替代模块。
  原因：Phase 1 只收敛 `mango-common`，具体校验能力应由后续业务域或独立能力按需设计。
- [x] 事项：不处理 gateway 默认服务名中的 `mango-user-starter`。
  原因：该项已登记为 Phase 5 输入，不属于 Phase 1。

## 被动适配

- [x] `mango-ai-api` 的 `ChatRequest` 使用 `jakarta.validation` 注解，但此前依赖由 `mango-common` 的 validation starter 间接提供；移除 common validation 依赖后，已在 `mango-ai-api/pom.xml` 显式补充 `jakarta.validation-api`。

## 验证结果

- `mvn -q -DskipTests compile`：通过。
- `mvn test`：未执行；Phase 1 验收命令未要求。
- 其它搜索/检查命令：
  - `find mango-common/src/main/java -type f | sort`：删除后 `mango-common` 剩余 7 个 Java 类。
  - `rg -n "io\\.mango\\.common\\.valid|PhoneValidator|IdCardValidator|@Phone|@IdCard" . --glob '!**/target/**'`：除 README 中删除记录外无代码引用。
  - `rg -n "spring-boot-starter-validation|jakarta\\.validation" mango-common/pom.xml mango-common/src/main/java --glob '!**/target/**'`：无命中。

## 遗留问题

- [x] 多个业务/API 模块仍使用 `jakarta.validation` 注解；本次只修复因移除 common 间接依赖而实际阻断编译的 `mango-ai-api`。其余模块当前 compile 通过，是否统一显式依赖可在对应模块 Phase 或依赖规则 Phase 中处理。
- [x] `Require` 仍保留在 `mango-common`；若后续出现复杂技术断言或领域断言扩展，应在对应 Phase 迁出。

## 下一 Phase 前置条件

- [x] Phase 2 可以在 `mango-common` 已收敛为公共契约的基础上进入 `mango-infra-kv`。
- [x] 后续模块不得再向 `mango-common` 添加业务 DTO / VO、技术实现或地区化校验实现。
- [x] 后续如新增公共契约，必须先按 `mango-common/README.md` 的准入规则说明使用方、边界和不可替代原因。
