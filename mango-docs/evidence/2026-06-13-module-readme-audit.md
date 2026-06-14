# 模块 README 能力说明审计

日期：2026-06-13

本报告记录本轮治理后的模块 README 审计结果。它是验收证据，不是规范源。

## 1. 审计目标

- 解决既有模块 README 参差不齐、更新不及时的问题。
- 让 Mango 开发者、业务开发者和 AI Agent 能从能力地图定位模块说明。
- 让公开能力变更后更新 README / 能力地图成为可执行门禁。
- 防止审计脚本只扫描已存在 README，漏掉没有 README 的模块。
- 对首批高价值前端组件、页面注册入口和业务接入入口建立组件级 README 基线。

## 2. 审计口径

执行命令：

```bash
node mango-pmo/tools/audit-module-readmes.mjs --self-test
node mango-pmo/tools/audit-module-readmes.mjs
```

脚本检查：

- 受管模块根目录是否存在 `README.md`。
- README 是否包含标准能力说明章节。
- 前端关键入口 README 是否包含入口定位、公开导出、接入方式、props/参数/事件、后端依赖、权限租户和验证方式。
- 是否存在 `TODO`、`TBD`、`待补充`、`待完善` 占位。
- 是否存在代码块命令再包反引号的格式错误。
- `pnpm -F @mango/* <script>` 是否引用了真实存在的 package script。
- `验证方式` 是否为空。
- `关联 PMO 规则` 是否链接 PMO 或业务 baseline 规则源。
- Markdown 相对链接和 `#anchor` 是否断裂。

## 3. 受管范围

- `mango/mango-platform/*`
- `mango/mango-infra/*`
- `mango-ui/packages/*`
- `mango/mango-admin-starter`
- `mango/mango-app`
- `mango/mango-common`
- `mango/mango-extension`
- `mango/mango-parent`
- `mango/mango-tools`
- `mango-business-starter`
- `mango-business-starter/business-pmo`
- `mango-business-starter/business-pmo/mango-baseline`
- `mango-business-starter/topologies/monolith`
- `mango-business-starter/topologies/microservice`
- 首批前端关键入口：`auth/src/views`、`file/src/components`、`job/src/views`、`rbac/src/views`、`system/src/components`、`workflow/src/components`

## 4. 结果摘要

| 指标 | 结果 |
|------|------|
| 受管 README 总数 | 67 |
| 缺失 README | 0 |
| 缺失标准章节 | 0 |
| 缺失前端入口章节 | 0 |
| 占位内容 | 0 |
| 反引号命令格式错误 | 0 |
| 不存在的前端 package script | 0 |
| 空验证方式 | 0 |
| PMO 规则链接缺失 | 0 |
| Markdown 文件或锚点断链 | 0 |

## 5. 本轮补齐内容

- 新增缺失 platform README：calendar、domain、file-preview、notice、numgen、org、seed、system、template。
- 新增缺失 infra README：context、fileproc、ip-location、log、module、sensitive、test。
- 新增缺失 frontend package README：admin、admin-pages、api-schema、app-runtime、auth、calendar、common、file、notice、numgen、rbac、system、template、workflow、workflow-business-example。
- 新增首批前端关键入口 README：auth views、file components、job views、rbac views、system components、workflow components。
- 新增缺失后端顶层 README：extension、parent、tools。
- 统一既有 admin-starter、app、common README 到新模板。
- 统一章节编号，把 `业务接入最小闭环` 固定为第 11 节。
- 能力地图升级为全量索引，并增加登录到菜单、文件到预览、审批、任务、业务项目到 PR 的组合接入入口。

## 6. 结论

当前能力说明已从“只覆盖部分核心 README”升级为“受管模块根目录 + 首批前端关键入口全覆盖”。后续新增模块、新增 frontend package 或改动纳入清单的前端关键入口没有 README 时，`audit-module-readmes.mjs` 会失败。

开发评审发现的 `@mango/api-schema` / `@mango/app-runtime` 验证命令问题已修正为真实可执行的 Node 断言；后续 README 中如果继续引用不存在的 `pnpm -F @mango/* <script>`，审计脚本会失败。
