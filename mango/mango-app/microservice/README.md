# Mango 微服务部署入口

最小微服务拓扑：

```text
请求
  -> mango-gateway-app
  -> mango-platform-app
  -> mango-business-app
```

## 应用

| App | 端口 | 职责 |
|-----|------|------|
| `mango-gateway-app` | `8080` | 网关与边界认证 |
| `mango-platform-app` | `8081` | 认证、身份、授权、组织、系统、验证码、消息 |
| `mango-business-app` | `8082` | 业务模块；后续业务 starter 添加到这里 |

## 规则

服务提供方 app 依赖本地 `*-starter`。服务调用方 app 依赖远程客户端 starter，例如 `mango-authorization-starter-remote`。
