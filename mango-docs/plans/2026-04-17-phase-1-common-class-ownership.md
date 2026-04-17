# Phase 1 mango-common 类归属表

- 盘点日期：2026-04-17
- 范围：`mango/mango-common/src/main/java`
- 基线命令：
  - `find mango-common/src/main/java -type f | sort`
  - `rg -n "io\\.mango\\.common" mango --glob '*.java' --glob '!mango-common/**'`

## 类清单

当前 `mango-common` 共有 11 个 Java 类。

| 类 | 外部依赖概况 | 归属 | 处理 | 理由 |
|----|--------------|------|------|------|
| `exception/BizException` | `mango-infra-web`、`mango-infra-security-starter` 使用；`Require` 内部使用 | 公共错误契约 | 保留 | 跨模块业务异常载体，依赖面稳定，只依赖 `CommonCode` |
| `po/PageQuery` | `mango-org-api` 使用；`mango-tools` 生成模板引用 | 分页请求契约 | 保留 | 只承载页码和页大小规范化，不含领域语义 |
| `result/BizCode` | `mango-org-api` 使用；`mango-tools` 生成模板引用 | 错误码接口契约 | 保留 | 允许各模块定义自己的错误码枚举，是稳定扩展点 |
| `result/CommonCode` | 仅 `BizException` / `R` / `Require` 内部使用 | 默认错误码基线 | 保留 | 作为公共默认错误码，边界清晰；不代表业务枚举 |
| `result/R` | app、infra、platform、tools 广泛使用 | API 返回契约 | 保留 | 当前所有 API 稳定依赖的统一响应结构 |
| `result/Require` | `mango-org-core` 使用；`mango-tools` 生成模板引用 | 轻量契约断言 | 保留 | 只生成 `BizException`，不依赖 Web/DB/Security；本阶段限制用途，不扩展为通用工具箱 |
| `valid/IdCard` | 无外部引用 | 地区化校验契约 | 删除 | 中国大陆身份证规则带地区/业务语义，且当前无消费者 |
| `valid/IdCardValidator` | 无外部引用 | 地区化校验实现 | 删除 | validator 是实现类，依赖 Jakarta Validation，不应为 common 引入运行时校验实现 |
| `valid/Phone` | 无外部引用 | 地区化校验契约 | 删除 | 中国大陆手机号规则带地区/业务语义，且当前无消费者 |
| `valid/PhoneValidator` | 无外部引用 | 地区化校验实现 | 删除 | validator 是实现类，依赖 Jakarta Validation，不应为 common 引入运行时校验实现 |
| `vo/PageResult` | `mango-org-api/core/starter` 使用；`mango-tools` 生成模板引用 | 分页返回契约 | 保留 | 只承载分页结果结构，不含领域语义 |

## 准入结论

保留范围：

- 统一返回结构：`R`
- 业务错误码契约：`BizCode`、`CommonCode`
- 业务异常契约：`BizException`
- 分页契约：`PageQuery`、`PageResult`
- 轻量契约断言：`Require`

删除范围：

- `@Phone` / `PhoneValidator`
- `@IdCard` / `IdCardValidator`

## 禁止进入 common

- `SysUser`、用户资料、用户角色关系等身份/授权侧模型。
- 角色、菜单、按钮权限、公共路径等权限模型。
- 组织、部门、岗位等组织模型。
- 消息、验证码、AI、租户、字典、配置、审计日志、路由等平台业务模型。
- Web、DB、Redis、Security、Crypto、Feign、Gateway 等技术实现。

## 后续说明

- 如后续需要手机号或身份证校验，应在具体领域模块或独立校验能力中定义；不得回填到 `mango-common`。
- 如后续发现 `Require` 承载复杂技术逻辑，应在对应 Phase 迁入更合适的模块；Phase 1 只保留其当前轻量契约断言能力。
