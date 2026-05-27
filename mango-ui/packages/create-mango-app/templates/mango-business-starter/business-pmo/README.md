# Business PMO

业务 PMO 只补充当前业务领域规则，不能放宽 Mango baseline 中的模块、API、数据库、测试和交付规则。

## 1. 目录职责

```text
business-pmo/
├── mango-baseline/     # 随业务项目带出的 Mango PMO 快照，只在升级 Mango 时更新
└── rules/              # 当前业务领域规则
```

`mango-baseline` 用于让业务仓在不包含 Mango 源码的情况下仍能执行 preflight 和交付台账检查。业务团队不得在普通业务需求中直接修改 `mango-baseline` 规则；需要调整框架规范时，应回到 Mango 仓库治理并通过版本升级带入。

## 2. 规则加载顺序

正式交付前：

1. 执行 `business-pmo/mango-baseline/tools/pmo-preflight.mjs`。
2. 阅读 preflight 输出的全部 Mango baseline 文件。
3. 阅读本文件。
4. 阅读 `rules/` 下与业务领域相关的规则。

## 3. 业务规则建议结构

推荐业务规则结构：

```text
business-pmo/
└── rules/
    └── {{moduleKebab}}/
        ├── 01-domain.md
        ├── 02-status.md
        └── 03-acceptance.md
```
