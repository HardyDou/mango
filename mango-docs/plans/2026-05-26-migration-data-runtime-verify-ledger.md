# Migration 数据运行验证台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MDR-001 | 用户要求 | 启动数据库并验证 migration 初始化正常 | 本地 `mango` 空库由 Flyway 初始化 | MySQL `mango` 数据库 | 12 个模块级 history 表均成功，除 baseline 外均为 V1 | DONE | `mango-docs/plans/2026-05-26-runtime-verify-app.log` |
| MDR-002 | 用户要求 | 验证菜单、功能按钮数据 | 查询授权菜单、按钮、权限码和父子引用 | `authorization_menu` 数据 | 菜单 194 条、按钮 151 条；父子引用/角色菜单引用/空权限/重复菜单码均为 0 | DONE | 本文件 |
| MDR-003 | 用户要求 | 验证角色、用户和基础数据 | 查询角色菜单、用户、成员、字典和模块基础表 | 授权/身份/系统基础数据 | 角色 4、角色菜单 638、用户 1、成员 4、字典类型 22、字典数据 78、API 资源 342 | DONE | 本文件 |
| MDR-004 | 用户要求 | 验证服务器可启动 | 关闭本机 office 插件依赖后启动单体服务 | 本地单体服务 | `/actuator/health` 200 UP、`/v3/api-docs` 200、`/auth/login` 200、`/auth/info` 200、`/authorization/menus/user` 200 | DONE | `mango-docs/plans/2026-05-26-runtime-verify-app.log` |
| MDR-005 | 规范要求 | 交付台账验证 | 执行 PMO delivery contract check | 台账检查结果 | 检查通过 | DONE | 本文件 |

## 验证备注

- 服务曾在 `127.0.0.1:5555` 成功监听，`/actuator/health` 返回 200 且数据库状态为 UP。
- 完成 HTTP 与数据验证后，为避免占用本机端口，已手动停止验证进程；日志末尾的 exit code 143 来自该停止动作，不代表启动失败。
- 本轮使用 `--office.plugin.enabled=false` 规避本机 office 插件依赖；未修改单体默认配置。
