# Mango - AI Native 开发底座

> 让 AI Agent 能够高效率、高质量地实现业务需求

## 项目组成

```
company02/
├── mango/          # Java 后端底座
├── mango-web/      # Vue 3 管理前端
└── mango-docs/     # 设计文档 & Sprint 计划
```

## 核心特性

| 特性 | 说明 |
|------|------|
| SPI + Starter | 部署拓扑无关，单体/微服务无缝切换 |
| DAL 抽象 | Redis/MySQL/Memory 通过接口访问 |
| @MangoTransactional | 本地/分布式事务配置切换 |
| M* 组件库 | 封装 Element Plus，统一前端规范 |
| AI Native | For AI Agent 的代码生成和检查 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.x + Spring Cloud Alibaba |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript |
| 数据库 | MySQL + MyBatis-Plus + Flyway |
| 注册/配置 | Nacos |
| 分布式事务 | Seata |
| 国密算法 | SM2/SM3/SM4 |

## 快速开始

### 后端

```bash
cd mango
# 查看规范
cat .claude/rules/01-code.md

# 代码检查
mvn mango:check
```

### 前端

```bash
cd mango-web
npm install
npm run dev
```

## 文档

- [后端规范](./mango/.claude/rules/)
- [前端规范](./mango-web/.claude/rules/)
- [Sprint 计划](./mango-docs/plans/)
