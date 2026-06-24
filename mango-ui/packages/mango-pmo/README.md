# @mango/pmo

## 1. 概览
`@mango/pmo` 是 Mango PMO baseline 的 npm 发布包。长期规则仍维护在仓库根目录 `mango-pmo`，本包只负责把这些规则、角色、模板和工具构建成可发布快照。

业务项目通过 `@mango/cli` 消费本包，不直接依赖包内脚本作为运行时代码。

## 2. 功能清单
| 能力 | 入口 | 说明 |
|------|------|------|
| 构建 baseline | `pnpm -F @mango/pmo build` | 复制 `mango-pmo` 到 `dist/baseline` |
| 校验 baseline | `pnpm -F @mango/pmo check` | 校验必备文件、manifest hash 和 preflight |
| 发布 manifest | `dist/baseline.json` | 记录 package version、文件列表和 SHA-256 |
| 业务同步 | `mango pmo sync/upgrade` | CLI 从本包安装业务仓 baseline |

## 3. 接入方式
Mango 发布前执行：

```bash
pnpm -F @mango/pmo build
pnpm -F @mango/pmo check
```

业务项目使用：

```bash
mango pmo check --project-dir .
mango pmo upgrade --project-dir .
```

## 4. 配置说明
| 配置入口 | 字段 | 含义 |
|----------|------|------|
| `package.json` | `files` | 发布 `dist`、README 和 package metadata |
| `package.json` | `exports` | 暴露 baseline manifest 和 baseline 文件 |
| `dist/baseline.json` | `packageVersion` | 当前 baseline 包版本 |
| `dist/baseline.json` | `files[].sha256` | 业务仓漂移检查依据 |

## 5. API 与扩展
| API / 扩展点 | 输入 | 输出 |
|--------------|------|------|
| `scripts/build-package.mjs` | `mango-pmo/**` | `dist/baseline/**`、`dist/baseline.json` |
| `scripts/check-package.mjs` | `dist/baseline/**` | 校验结果 |
| `exports["."]` | npm import | `dist/baseline.json` |
| `exports["./baseline/*"]` | npm package path | baseline 文件 |

## 6. 数据与初始化
本包不初始化数据库、菜单、权限、租户或业务数据。

| 类型 | 位置 | 初始化方式 |
|------|------|------------|
| PMO baseline | `dist/baseline` | build 脚本从 `mango-pmo` 复制 |
| baseline manifest | `dist/baseline.json` | build 脚本按文件内容生成 hash |
| 业务项目 baseline | `business-pmo/mango-baseline` | `@mango/cli` 安装或升级 |

## 7. 管理入口
| 任务 | 命令 |
|------|------|
| 构建包 | `pnpm -F @mango/pmo build` |
| 校验包 | `pnpm -F @mango/pmo check` |
| 发布包 | `pnpm publish:pkg pmo --dry-run` |
| 业务升级 | `mango pmo upgrade --project-dir .` |

## 8. 快速开始
1. 修改根目录 `mango-pmo/**`。
2. 执行 `pnpm -F @mango/pmo build`。
3. 执行 `pnpm -F @mango/pmo check`。
4. 发布前执行 `pnpm -F @mango/pmo pack --dry-run` 确认 tarball 内容。
5. 业务项目通过 `mango pmo upgrade` 获取新 baseline。

## 9. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `dist/baseline.json` 不存在 | 未执行 build | 执行 `pnpm -F @mango/pmo build` |
| check 报 hash mismatch | dist 内容和 manifest 不一致 | 重新 build 后再 check |
| 业务项目 baseline changed | 业务仓 baseline 被改或版本落后 | 执行 `mango pmo upgrade --project-dir .` |
| npm tarball 缺 baseline | 发布前未 build 或 files 配置错误 | 执行 pack dry-run 并检查 `package.json` |

## 10. 相关文档
- [Mango PMO Baseline](../../../mango-pmo/README.md)
- [@mango/cli](../mango-cli/README.md)
- [PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
- [开发环境规范](../../../mango-pmo/rules/02-dev-environment.md)
