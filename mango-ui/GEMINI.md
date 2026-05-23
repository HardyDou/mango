# Mango UI Gemini 入口

@../mango-pmo/rules/frontend/05-dev-flow.md
@../mango-pmo/rules/frontend/01-vue-code.md
@../mango-pmo/rules/frontend/06-monorepo-architecture.md
@../mango-pmo/rules/frontend/04-test.md

进入 `mango-ui` 前端子项目后，先按 `../AGENTS.md` 执行 PMO preflight。

推荐命令：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango-ui/**"
```

读取 preflight 输出中 `Must read` 的每一个文件原文后，再开始设计、编码或验证。
