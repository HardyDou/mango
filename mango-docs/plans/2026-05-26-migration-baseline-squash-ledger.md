# Migration 基线压缩交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MBS-001 | 用户要求 | 没有正式发布、无共享环境依赖，可以压缩 migration | 多版本模块压缩为模块级 V1 | 多模块 migration SQL | 空库 Flyway 启动验证通过，12 个模块 history table 均只记录 V1 | DONE | `mango/**/src/main/resources/db/migration` |
| MBS-002 | 用户要求 | 本地库 drop 重建 | 先备份，再 drop/create 本地 `mango` 库 | 本地 MySQL `mango` | MySQL 命令与启动验证 | DONE | `mango-docs/db-backups` |
| MBS-003 | 规范要求 | 验证改动 | 执行 Flyway 启动验证、compile 和台账检查 | 验证命令输出 | `mvn -f mango/pom.xml compile -DskipTests` 通过；启动停在本机 office 组件缺失，Flyway 已完成 | DONE | 本文件 |
