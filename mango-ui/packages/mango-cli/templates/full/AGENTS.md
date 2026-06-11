# Mango Business Agent 入口

本项目由 `mango-cli init --preset {{preset}}` 生成。

## 1. 唯一规范源

- `business-pmo/mango-baseline` 是当前业务仓的 Mango baseline 规范快照。
- `business-docs` 只放业务设计文档、Sprint 计划、交付记录和历史设计，不作为长期规范源。
- `AGENTS.md` 只做入口和路由，不复制长期规则正文。

## 2. PMO preflight

正式开发、验证、发布、提交前执行：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role <pm|tech-lead|dev|qa|pmo> \
  --phase <requirement|design|develop|verify|release|governance> \
  --task "<任务>" \
  --paths "<影响路径，逗号分隔>"
```

然后读取输出中 `Must read` 的每一个文件原文。

## 3. 交付报告

最终回复必须包含：

- 改动范围。
- 实际加载的 Mango baseline 文件。
- 执行的验证命令。
- 未验证项和风险。
- PMO 例外说明；没有例外则写“无”。

## 4. 验收证据

涉及页面、接口、权限、数据或 E2E 验收时，必须填写验收证据，并执行：

```bash
node business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs \
  --evidence "<验收证据文件路径>"
```

禁止只用“接口 200”“页面无异常”“截图正常”声明验收通过。

## 5. 本地开发启动

后端开发只使用：

```bash
scripts/dev-workspace.sh start
```

不要用 `java -jar` 或手写 Maven reactor 命令作为开发启动入口；这些细节由脚本封装。
