# 业务域管理模块交付台账

## 1. 交付范围

| 项 | 内容 | 状态 |
|---|---|---|
| 后端模块 | `mango-domain-api/core/starter/starter-remote` | DONE |
| 数据库 | `biz_domain` migration 和内置业务域 seed | DONE |
| 菜单权限 | 系统设置“业务域”菜单和权限 migration | DONE |
| 前端页面 | 业务域管理页面 | DONE |
| 前端组件 | `DomainSelector` | DONE |
| 后续 issue | 字典、系统参数/配置、工作流、通知适配 | DONE |

## 2. 验证记录

| 命令 | 状态 | 说明 |
|---|---|---|
| `mvn -pl mango-platform/mango-domain/mango-domain-starter -am test` | PASS | 业务域本地 starter 编译和依赖测试 |
| `mvn -pl mango-platform/mango-domain/mango-domain-starter-remote -am test` | PASS | 业务域 remote starter 编译 |
| `mvn -pl mango-platform/mango-system/mango-system-starter -am test` | PASS | 字典、系统参数/配置适配编译和系统模块测试 |
| `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -am test` | PASS | 工作流业务域适配编译和工作流模块测试 |
| `mvn -pl mango-platform/mango-notice/mango-notice-starter -am test` | PASS | 通知业务域适配编译和通知模块测试 |
| `pnpm -C mango-ui --filter @mango/system build` | PASS | 业务域页面、选择器、字典、系统参数/配置页面构建 |
| `pnpm -C mango-ui --filter @mango/workflow build` | PASS | 工作流业务域适配页面构建 |
| `pnpm -C mango-ui --filter @mango/notice build` | PASS | 通知业务域适配页面构建 |
| `pnpm -C mango-ui --filter @mango/admin-pages build` | PASS | 后台页面注册包构建 |

说明：曾执行 `mvn -pl mango-platform/mango-domain -am test`，该命令只构建聚合 POM，不作为有效验证口径；已改用 `mango-domain-starter` 和 `mango-domain-starter-remote`。

## 3. PMO 例外

无。
