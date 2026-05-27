# Business PMO

业务 PMO 只补充当前业务领域规则，不能放宽 Mango baseline 中的模块、API、数据库、测试和交付规则。

推荐结构：

```text
business-pmo/
└── rules/
    └── {{moduleKebab}}/
        ├── 01-domain.md
        ├── 02-status.md
        └── 03-acceptance.md
```
