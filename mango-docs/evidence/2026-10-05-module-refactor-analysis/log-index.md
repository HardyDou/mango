# 模块重构分析验证日志索引

> 运行日志保存在本机 `/tmp`，不提交到仓库。路径不包含凭据；数据库密码、token 和密钥不在索引中。

| 证据 | 本机路径 | 用途 | 结果 |
|---|---|---|---|
| 最终 Java 21/Redis/MySQL Reactor | `/tmp/mango-clean-verify-final-java21-redis-20261005-8.log` | `mvn clean verify`、全量编译/测试/打包 | `BUILD SUCCESS`，MAVEN_EXIT_CODE=0 |
| 前次全量诊断日志 | `/tmp/mango-clean-verify-final-java21-redis-20261005-7.log` | 文件预览失败修复前后对照 | 后续 8 号日志为最终结果 |
| Bootstrap/API 诊断日志 | `/tmp/mango-clean-verify-final-java21-redis-20261005-6.log` | Bootstrap declaration 与相关 Reactor 诊断 | 非最终日志；最终结果见 8 号日志 |
| 初次全量诊断日志 | `/tmp/mango-clean-verify-final-java21-redis-20261005-5.log` | 记录固定 API version 断言问题 | 非最终日志；问题已由注解版本契约修复 |
| File Preview app Flow | `/tmp/mango-file-preview-app-flow-20261005-4.log` | 独立文件预览应用直接预览、302、外部 `/api` 前缀 | 5/5 PASS |
| 测试质量检查 | 命令输出 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | `PASS: 14 file(s)` |
| README 模块审计 | 命令输出 | `node mango-pmo/tools/audit-module-readmes.mjs` | PASS |
| README 源事实审计 | 命令输出 | `node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS |
| 工作区差异检查 | 命令输出 | `git diff --check` | PASS |

最终验收、skipped 明细、数据库/Bootstrap/API/E2E 回读和三轮角色视角评审见 [`acceptance-evidence.md`](./acceptance-evidence.md)。
