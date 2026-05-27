# {{moduleName}} {{aggregatePascal}} 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| BIZ-001 | 业务需求 | 新增 {{aggregatePascal}} API | 使用 `{{modulePascal}}Api` 和 `R<T>` | `{{moduleKebab}}-api` | Maven test | REQUESTED |  |
| BIZ-002 | 业务需求 | 新增 {{aggregatePascal}} 页面 | 使用 page registry 注册 | `frontend/packages/{{moduleKebab}}` | pnpm build | REQUESTED |  |
| BIZ-003 | 业务需求 | 同步菜单和权限 | 使用 resource manifest | `resource-manifest.json` | 资源同步验证 | REQUESTED |  |
