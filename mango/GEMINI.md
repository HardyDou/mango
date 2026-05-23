# Mango 后端 Gemini 入口

@../mango-pmo/rules/backend/10-dev-flow.md
@../mango-pmo/rules/backend/01-code.md
@../mango-pmo/rules/backend/05-module.md
@../mango-pmo/rules/backend/08-test.md

进入 `mango` 后端子项目后，先按 `../AGENTS.md` 执行 PMO preflight。

推荐命令：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango/**"
```

读取 preflight 输出中 `Must read` 的每一个文件原文后，再开始设计、编码或验证。
