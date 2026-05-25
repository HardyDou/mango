# Mango 后端 Claude 入口

@../mango-pmo/rules/backend/10-dev-flow.md
@../mango-pmo/rules/backend/01-code.md
@../mango-pmo/rules/backend/02-naming.md
@../mango-pmo/rules/backend/05-module.md
@../mango-pmo/rules/backend/08-test.md

Java 后端脚手架，AI Agent 高效率编码指南。进入 `mango` 后端子项目后，先按 `../AGENTS.md` 判断是否需要执行 PMO preflight。

需要执行 preflight 时，推荐命令：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango/**"
```

读取 preflight 输出中 `Must read` 的每一个文件原文后，再开始设计、编码或验证。简单问答、只读定位和快速查看不需要 preflight。

## 2. 后端核心原则

| 原则 | 说明 |
|------|------|
| SPI + Starter | SPI 机制 + Starter 模式，模块可插拔切换 |
| 模块拆分 | 业务能力通常划分为 api/core/starter/starter-remote；边界入口等运行时基础设施按实际形态拆分为 web-starter、gateway-starter 等，不滥用 remote 命名 |
| DAL 层抽象 | Redis/DB 必须通过 ICache/ILocker 接口 |
| 禁止条件分支 | 统一 SPI 注入，不用 if |
| TTL 配置化 | 缓存超时禁止硬编码 |
| DDL Flyway | 数据库变更必须 migration 文件 |
| 中文注释 | 新增和修改的代码注释、JavaDoc、README 与交付文档默认使用中文 |
| 作者标识 | 新增 Java 类型如需 `@author`，必须使用当前系统用户；代码生成器自动从系统用户提取 |

## 3. 专项规则

涉及以下能力时，必须通过 preflight 或手动读取对应 PMO 规则：

- API：`../mango-pmo/rules/backend/03-api.md`
- 数据库：`../mango-pmo/rules/backend/04-db.md`
- 安全：`../mango-pmo/rules/backend/06-security.md`
- 事务：`../mango-pmo/rules/backend/07-persistence.md`
- 版本化发布：`../mango-pmo/rules/backend/09-versioning.md`

## 4. 常用命令

```bash
mvn spring-boot:run
mvn test
mvn verify
mvn mango:check
```
