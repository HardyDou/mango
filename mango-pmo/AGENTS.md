# Mango PMO Agent 入口

进入 `mango-pmo` 后，所有任务都按规范治理处理。

## 1. 推荐 preflight

```bash
node tools/pmo-preflight.mjs \
  --role pmo \
  --phase governance \
  --task "<用户任务>" \
  --paths "mango-pmo/**"
```

## 2. 治理原则

- `rules/**` 是唯一长期规则位置。
- `agents/**` 只定义角色职责和执行方式。
- 新规则只写一次，不在 `mango-docs`、入口文件或设计文档中复制长期规则。
- 入口文件只做 Agent 兼容和规则路由。

## 3. 交付要求

调整 PMO 后必须运行 preflight 校验，并说明新增或调整的规则入口。
