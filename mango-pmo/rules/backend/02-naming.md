---
paths:
  - "**/*.java"
  - "**/pom.xml"
  - "**/*.xml"
---

# 后端命名规范 (backend-naming-rules)

## 1. 目标

本规范用于统一 Mango 后端的命名语言，减少以下问题：

- 同一个对象在不同模块里含义漂移
- `PO` / `DTO` / `DO` / `VO` 混用，AI 和开发者都难以判断语义
- 包名、模块名、表名、接口名彼此不一致

核心原则：

- 名称优先表达职责，不优先表达技术细节
- 同一类对象在全仓必须使用同一后缀
- 不再使用 `BasePO` / `BaseVO` 作为通用兜底命名

---

## 2. 基本风格

| 对象 | 规范 | 示例 |
|------|------|------|
| Maven 模块 | `kebab-case` | `mango-org-api` |
| Java 包名 | `lowercase` | `io.mango.org.api.query` |
| Java 类名 | `PascalCase` | `CreatePostCommand` |
| 方法/变量 | `camelCase` | `getById`, `postPageQuery` |
| 常量 | `UPPER_SNAKE_CASE` | `DEFAULT_PAGE_SIZE` |
| 数据库表名 | `snake_case` | `org_post` |
| 数据库字段 | `snake_case` | `post_name` |

禁止：

- `user_info`, `User_info`, `userInfoDTO` 这类风格混杂
- 拼音缩写、业务黑话、无语义缩写
- `data`, `info`, `temp`, `obj` 这类泛化命名

---

## 3. 领域名统一

同一业务概念在文档、模块、包、类、表中必须保持同一领域词。

| 领域 | 模块 | 包 | 表前缀 | 示例类 |
|------|------|----|--------|--------|
| 组织 | `mango-org` | `io.mango.org` | `org_` | `OrgApi` |
| 权限 | `mango-rbac` | `io.mango.rbac` | `rbac_` | `SysRoleVO` |
| 系统 | `mango-system` | `io.mango.system` | `sys_` | `SysConfigVO` |
| 区域 | `mango-area` | `io.mango.area` | `area_` | `AreaApi` |

禁止：

- 模块叫 `org`，包里却叫 `user`
- 表前缀与模块领域不一致
- 文档、POM、代码里对同一模块使用不同名称

---

## 4. 对象模型命名

这是本规范的核心。

### 4.1 统一后缀

| 对象类型 | 后缀 | 位置 | 含义 |
|----------|------|------|------|
| 持久化实体 | `Entity` | `core/entity` | 对应数据库表结构 |
| 分页查询 | `PageQuery` | `api/query` | 带分页能力的查询参数 |
| 普通查询 | `Query` | `api/query` | 非分页查询参数 |
| 新增命令 | `Create...Command` | `api/command` | 创建资源入参 |
| 更新命令 | `Update...Command` | `api/command` | 更新资源入参 |
| 视图返回 | `VO` | `api/vo` | 对外返回对象 |
| 领域接口 | `XxxApi` | `*-api` | 跨模块暴露接口 |
| 内部服务接口 | `IXxxService` | `*-core` | 本模块内部服务 |
| Provider 类接口 | `I...Provider` | `*-api` 或 infra API | 可插拔能力接口 |

### 4.2 明确禁止

以下后缀禁止继续作为默认业务对象命名：

- `PO`
- `DO`
- `DTO` 作为仓内业务 API 入参/返回命名

解释：

- `PO` 容易在“接口入参”和“持久化对象”之间歧义
- `DO` 在仓库里没有稳定共识
- Mango 仓内业务 API 返回统一使用 `VO`，入参统一使用 `Command` / `Query`
- `DTO` 只适合少数明确的数据传输场景，不适合作为仓内业务 API 的统称

### 4.3 `DTO` 的保留场景

`DTO` 不是完全禁用，只允许用于以下场景：

- 明确的第三方集成传输对象
- 明确的第三方适配层临时对象
- 历史接口兼容层，且有迁移计划

示例：

- `WechatTemplateMessageDTO`
- `PaymentGatewayCallbackDTO`
- `UserExportDTO`

禁止：

- `CreateUserDTO`
- `UserPageDTO`
- `UserDTO` 作为仓内业务 API 返回对象
- `UserDTO` 作为仓内业务 API `@RequestBody`

### 4.4 推荐组合

以“岗位 Post”领域为例：

```java
PostEntity
PostPageQuery
CreatePostCommand
UpdatePostCommand
PostVO
IPostService
PostApi
```

分页统一复用公共契约：

```java
PageQuery
PageResult<PostVO>
```

补充约束：

- `R<T>` 的 `T` 如果是业务对象，统一使用 `VO`
- 禁止 `R<UserDTO>`、`R<PageResult<UserDTO>>`
- 禁止 `create(UserDTO dto)`、`update(UserDTO dto)` 这类仓内业务 API 命名

---

## 5. 包命名规范

遵循 `05-module.md` 的分层前提下，包名统一如下：

### 5.1 `*-api`

```
io.mango.xxx.api
├── command
├── query
├── vo
└── enums
```

说明：

- `command` 只放写操作入参
- `query` 只放查询入参
- `vo` 只放对外返回
- `enums` 只放领域枚举与错误码枚举

### 5.2 `*-core`

```
io.mango.xxx.core
├── entity
├── service
├── service.impl
├── mapper
├── convert
└── manager
```

说明：

- `convert` 用于对象转换
- `manager` 仅用于复杂领域编排，不得沦为万能杂物层

### 5.3 `*-starter`

```
io.mango.xxx.starter
├── controller
└── config
```

### 5.4 `*-starter-remote`

```
io.mango.xxx.starter.remote
```

禁止：

- `util`、`helper`、`common` 作为领域模块里的默认杂项包
- `po`、`dto`、`model` 这种语义模糊包名继续扩散

---

## 6. 接口与实现命名

### 6.1 对外接口

跨模块暴露接口统一使用 `XxxApi`。

```java
public interface OrgApi
public interface SysRoleApi
public interface AuthApi
```

要求：

- 名称表达能力，不表达实现方式
- 一个接口聚焦一个领域能力，不要做大而全总入口

### 6.2 内部服务接口

本模块内部服务统一使用 `IXxxService`。

```java
public interface ISysOrgService
public interface IPostService
```

实现类统一使用 `XxxServiceImpl`。

```java
public class SysOrgServiceImpl implements ISysOrgService
```

### 6.3 可插拔能力接口

面向扩展点的能力接口统一使用：

- `I...Provider`
- `I...Checker`
- `I...Validator`
- `I...Resolver`

例如：

```java
IUserContextProvider
IPermissionChecker
ITokenValidator
ITenantResolver
```

---

## 7. Controller / Mapper / Convert 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | `XxxController` | `PostController` |
| Mapper | `XxxMapper` | `PostMapper` |
| 转换器 | `XxxConvert` | `PostConvert` |
| 装配器 | `XxxAssembler` | `PostAssembler` |
| 配置类 | `XxxAutoConfiguration` | `MangoOrgAutoConfiguration` |

转换命名要求：

- `toVO`
- `toEntity`
- `toCreateCommand`
- `toUpdateCommand`

禁止：

- `build`, `wrap`, `parse`, `handle` 这类语义过宽的方法名泛滥

---

## 8. 方法命名

### 8.1 查询类方法

| 场景 | 规范 | 示例 |
|------|------|------|
| 主键查询 | `getById` | `getById(Long id)` |
| 条件查询单条 | `findBy...` | `findByCode(String code)` |
| 条件查询列表 | `listBy...` | `listByTenantId(Long tenantId)` |
| 分页查询 | `page` | `page(PostPageQuery query)` |
| 是否存在 | `existsBy...` | `existsByRoleCode(String roleCode)` |

### 8.2 写操作方法

| 场景 | 规范 | 示例 |
|------|------|------|
| 创建 | `create` / `save` | `create(CreatePostCommand command)` |
| 更新 | `update` | `update(UpdatePostCommand command)` |
| 删除 | `delete` / `remove` | `delete(Long id)` |
| 启用 | `enable` | `enable(Long id)` |
| 禁用 | `disable` | `disable(Long id)` |

要求：

- 查询方法名不要写成写操作语义
- 写操作方法必须体现动作，不要用 `submit`、`deal`、`process`

---

## 9. 数据库命名

### 9.1 表命名

表名统一：`领域前缀_业务名`

| 模块 | 表前缀 | 示例 |
|------|--------|------|
| `org` | `org_` | `org_post` |
| `rbac` | `rbac_` | `rbac_role` |
| `system` | `sys_` | `sys_config` |
| `area` | `area_` | `area_region` |

### 9.2 字段命名

统一使用 `snake_case`：

- `id`
- `tenant_id`
- `post_name`
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`
- `deleted`

### 9.3 主键与外键字段

| 场景 | 规范 | 示例 |
|------|------|------|
| 主键 | `id` | `id` |
| 外键 | `xxx_id` | `role_id`, `tenant_id` |
| 编码 | `xxx_code` | `post_code` |
| 名称 | `xxx_name` | `post_name` |
| 状态 | `status` 或 `xxx_status` | `status`, `audit_status` |

禁止：

- 同表中同时存在 `postId` 和 `post_id`
- 同语义字段在不同模块里一会儿 `name` 一会儿 `title`

---

## 10. 枚举与错误码命名

### 10.1 枚举类

统一使用 `XxxEnum` 或 `XxxCode`。

```java
PostStatusEnum
TenantTypeEnum
PostCode
```

### 10.2 错误码枚举

领域错误码统一使用 `XxxCode`，并实现 `BizCode`。

```java
public enum PostCode implements BizCode
```

不要使用：

- `XxxErrorEnum`
- `XxxStatusCodeEnum`

---

## 11. 统一示例

以“岗位”场景为标准示例：

```java
// api
io.mango.org.api.query.PostPageQuery
io.mango.org.api.command.CreatePostCommand
io.mango.org.api.command.UpdatePostCommand
io.mango.org.api.vo.PostVO
io.mango.org.api.enums.PostCode
io.mango.org.api.PostApi

// core
io.mango.org.core.entity.PostEntity
io.mango.org.core.mapper.PostMapper
io.mango.org.core.service.IPostService
io.mango.org.core.service.impl.PostServiceImpl
io.mango.org.core.convert.PostConvert

// starter
io.mango.org.starter.controller.PostController
```

接口签名示例：

```java
PageResult<PostVO> page(PostPageQuery query);
PostVO getById(Long id);
void create(CreatePostCommand command);
void update(UpdatePostCommand command);
void delete(Long id);
```

---

## 12. 迁移规则

旧命名迁移到新命名时，按以下规则执行：

| 旧命名 | 新命名 |
|--------|--------|
| `XxxPO` | `XxxEntity` / `XxxPageQuery` / `CreateXxxCommand` / `UpdateXxxCommand`，按真实职责拆分 |
| `BasePO` | 删除，不保留 |
| `XxxDTO` | 若为接口入参/返回，改成 `Command` / `Query` / `VO` |
| `BaseVO` | 删除，不保留 |
| `PageVO` | `PageResult<T>` |
| `PageParam` / `PagePO` | `PageQuery` 或 `XxxPageQuery` |

迁移要求：

- 不允许为了兼容长期保留双套命名
- 如果一个旧对象同时承担多个职责，必须拆分，不允许只改类名

---

## 13. 审查清单

提交前至少自查以下问题：

- 这个类名能否让人一眼看出职责？
- 这个对象到底是实体、查询、命令还是返回？
- 同一领域词在模块、包、表、类中是否一致？
- 有没有继续引入 `PO` / `DO` / 泛化 `DTO`？
- 有没有出现 `model`、`common`、`util` 这种逃避命名？

如果以上任一问题回答不清楚，说明命名还不合格。
