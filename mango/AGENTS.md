# Mango 后端 Agent 入口

进入 `mango` 后端子项目后，仍以根目录 `AGENTS.md` 的 PMO preflight 为第一入口。

## 1. 推荐 preflight

后端开发、修复或重构任务优先执行：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango/**"
```

涉及设计时使用 `--role tech-lead --phase design`；涉及验收时使用 `--role qa --phase verify`。

## 2. 后端默认必读

preflight 至少应覆盖：

- `rules/00-dev-flow.md`
- `rules/backend/10-dev-flow.md`
- `rules/backend/01-code.md`
- `rules/backend/02-naming.md`
- `rules/backend/05-module.md`
- `rules/backend/08-test.md`

涉及 API、数据库、安全、事务、版本化发布时，必须读取对应专项规则。

## 3. 后端硬约束

- 新增后端能力必须明确模块边界、API、数据变化和测试范围。
- DDL 变更必须使用 Flyway migration。
- 后端 `Long`、雪花 ID、业务主键对前端输出时按字符串处理。
- 有发布态和历史版本的领域对象，按版本化发布规范处理。
- 不用 mock 替代被测实现本身；集成行为使用真实集成物料或等价容器/嵌入式环境。
