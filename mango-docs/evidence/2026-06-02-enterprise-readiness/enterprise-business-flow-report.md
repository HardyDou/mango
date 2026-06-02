# 企业业务项目全流程验证报告

## 验证目标

- 使用 `mango-cli` 生成独立企业业务项目，验证企业不直接在 Mango 主仓上开发业务时，是否能依赖发布物料完成初始化。
- 在生成项目中新增真实业务模块，检查后端持久化 CRUD、前端页面/API、菜单权限、PMO 规范和构建链路是否可用。
- 发现问题先登记，不用假数据或跳过构建来声明通过。

## 验证项目

- 项目名：`contract-ops-platform`
- 业务场景：采购订单管理
- CLI 初始化参数：`--preset full --topology monolith --package com.acme.contractops --group-id com.acme`
- 新增模块参数：`module add procurement --aggregate order --module-name 采购管理`

## 已通过项

| 项目 | 结果 | 证据 |
|---|---|---|
| CLI 自测 | 通过 | `pnpm --dir mango-ui --filter mango-cli test` 输出 `mango-cli full/custom/add/module checks passed.` |
| 项目初始化 | 通过 | 生成 `frontend`、`backend`、`business-pmo`、`business-docs`、`topologies`、`mango.config.json` |
| 新增业务模块 | 通过 | 生成 `backend/modules/procurement` 和 `frontend/packages/procurement*` |
| 占位和废弃业务残留 | 通过 | `rg "\\{\\{|guarantee|保函|担保|baohan"` 无输出 |
| 后端真实 CRUD 骨架 | 通过 | Controller 继承 `BaseCrudController`；Service 继承 `MangoCrudServiceImpl`；Entity 继承 `TenantEntity`；Flyway 创建 `procurement_order` |
| 菜单和页面注册 | 通过 | `resource-manifest.json` component 为 `procurement/order/index`；前端 `registerModulePages` 注册同名 key |
| 业务 PMO 激活 | 通过 | 生成项目 preflight 能按业务模块路径要求读取前后端、数据库、测试规范 |
| 后端构建 | 通过 | `mvn -f backend/pom.xml -DskipTests package` 成功 |
| 前端依赖安装 | 通过 | `npm --prefix frontend install --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/` 成功 |
| 前端 dev 启动 | 通过 | `npm --prefix frontend run dev` 启动到 `http://localhost:5176/` |

## 未通过项

| 问题 | 现象 | 影响 | 建议处理 |
|---|---|---|---|
| 前端 typecheck/build 失败 | `node_modules/@mango/common/utils/message.ts(27,20): error TS18048: 'message' is possibly 'undefined'.` | 企业项目不能按 README 执行 `npm run build`，阻断正式推广 | 修复 `@mango/common` 发布包出口：子路径不要直接暴露源码 `.ts`，或模板改用稳定根入口；修复后重新发布前端包并回归企业项目 build |
| 前端业务 CRUD UI 不完整 | 生成页面当前只有查询、新增、列表展示；API 有详情，后端 BaseCrudController 支持更新/删除，但页面没有详情/编辑/删除入口 | “低成本新增业务模块”可用，但不能声明企业开发者拿到完整 CRUD 页面骨架 | 补齐模板的详情、编辑、删除、分页和确认交互，并添加 CLI 模板测试 |
| 后端依赖仍是 SNAPSHOT | `backend/pom.xml` 中 `mango.version` 为 `1.0.0-SNAPSHOT` | 企业正式使用会依赖快照发布物，不利于版本锁定和问题追踪 | 推广前切到已发布 release 版本，避免前端包、后端 starter、CLI 模板版本不匹配 |

## 结论

ER-013 已真实执行，但不能置为 DONE。当前 Mango 已具备企业业务项目初始化、业务模块生成、PMO 规范带出和后端构建能力；正式推广前必须先修复前端发布包出口导致的 build 失败，并补齐业务 CRUD 页面骨架的验收能力。
