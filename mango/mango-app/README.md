# Mango App

`mango-app` 只定义可部署的 Spring Boot 启动入口。业务规则必须放在
`mango-platform`、`mango-infra` 或后续业务能力模块中。

## 目录结构

```text
mango-app/
├── monolith/
│   └── mango-monolith-app
└── microservice/
    ├── mango-gateway-app
    ├── mango-platform-app
    └── mango-business-app
```

## 依赖规则

| App | 用途 | 依赖方式 |
|-----|------|----------|
| `mango-monolith-app` | 单进程本地部署 | 依赖本地 `*-starter` |
| `mango-gateway-app` | 外部流量入口 | 依赖网关远程 starter |
| `mango-platform-app` | 平台支撑能力服务 | 依赖本地平台 starter |
| `mango-business-app` | 业务能力服务组 | 依赖业务 starter 和平台远程 starter |

旧的顶层 `mango-admin-app` 已废弃，由 `monolith/mango-monolith-app` 替代。
