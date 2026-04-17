---
paths:
  - "**/*Api.java"
  - "**/*Controller.java"
  - "**/api/command/*.java"
  - "**/api/query/*.java"
  - "**/api/vo/*.java"
---

# 后端 API 规范 (backend-api-rules)

## 1. 目标

本规范用于统一 Mango 后端 API 的协议边界，解决以下问题：

- Controller 直接暴露 `PO` / `Entity`
- 写接口入参与查接口入参混用
- 返回对象命名漂移，`DTO` / `VO` 混用
- API 层与持久化层、领域实现层边界不清

核心原则：

- API 只暴露协议模型，不暴露持久化模型
- 写操作用 `Command`，查操作用 `Query`
- 返回对象统一使用 `VO`
- 仓内业务 API 不使用 `DTO` 作为默认入参或返回命名

---

## 2. API 协议模型

### 2.1 入参分类

| 场景 | 类型 | 位置 | 示例 |
|------|------|------|------|
| 创建资源 | `Create...Command` | `api/command` | `CreatePostCommand` |
| 更新资源 | `Update...Command` | `api/command` | `UpdatePostCommand` |
| 普通查询 | `...Query` | `api/query` | `UserQuery` |
| 分页查询 | `...PageQuery` | `api/query` | `PostPageQuery` |
| 简单路径参数 | 标量 | 方法签名 | `@PathVariable Long id` |
| 简单查询参数 | 标量 | 方法签名 | `@RequestParam String code` |

规则：

- `@RequestBody` 只接收 `Command` 或 `Query`
- `GET` 默认不用 `@RequestBody`
- 简单参数不用额外包一层 `Request`
- 禁止直接把 `PO` / `Entity` / `Map<String, Object>` 作为通用业务接口入参

### 2.2 返回分类

| 场景 | 类型 | 示例 |
|------|------|------|
| 无数据成功 | `R<Void>` | 删除、保存成功 |
| 单对象返回 | `R<XxxVO>` | `R<PostVO>` |
| 列表返回 | `R<List<XxxVO>>` | `R<List<SysRoleVO>>` |
| 分页返回 | `R<PageResult<XxxVO>>` | `R<PageResult<PostVO>>` |
| 状态型返回 | `R<Boolean>` | 启停、校验结果 |
| 简单标识返回 | `R<Long>` / `R<String>` | 新建资源 ID |

规则：

- 仓内业务 API 返回统一使用 `VO`
- 禁止返回 `PO` / `Entity`
- `PageResult` 的泛型统一为 `XxxVO`
- 只有非常简单的状态型或标识型场景允许返回基础类型

---

## 3. 命名约束

### 3.1 强制规则

- 返回统一叫 `XxxVO`
- 仓内业务 API 禁止使用 `XxxDTO` 作为返回对象
- 仓内业务 API 禁止使用 `XxxDTO` 作为 `@RequestBody`
- 持久化对象统一叫 `XxxEntity`
- 写接口入参统一叫 `CreateXxxCommand` / `UpdateXxxCommand`
- 查接口入参统一叫 `XxxQuery` / `XxxPageQuery`

### 3.2 `DTO` 的唯一保留场景

`DTO` 只允许出现在以下场景：

- 第三方集成协议对象
- 外部网关回调对象
- 历史兼容层的临时传输对象，且必须有迁移计划

示例：

- `WechatTemplateMessageDTO`
- `PaymentGatewayCallbackDTO`

禁止：

- `UserDTO`
- `CreateUserDTO`
- `UserPageDTO`

---

## 4. Controller / Api 分层约束

### 4.1 `*-api` 层允许定义的内容

```
io.mango.xxx.api
├── XxxApi.java
├── command/
├── query/
├── vo/
└── enums/
```

允许：

- `XxxApi`
- `command`
- `query`
- `vo`
- 领域枚举、错误码

禁止：

- `entity`
- `mapper`
- `service`
- `controller`

### 4.2 Controller 约束

- Controller 只负责 HTTP 协议适配
- Controller 实现 `XxxApi`
- Controller 不直接操作 `Mapper`
- Controller 不直接组装 `Entity`
- Controller 不返回 `Entity` / `PO`

---

## 5. 标准示例

### 5.1 API 接口

```java
package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import org.springframework.web.bind.annotation.*;

public interface PostApi {

    @GetMapping("/org/post/page")
    R<PageResult<PostVO>> page(PostPageQuery query);

    @GetMapping("/org/post/{id}")
    R<PostVO> get(@PathVariable Long id);

    @PostMapping("/org/post")
    R<Void> create(@RequestBody CreatePostCommand command);

    @PutMapping("/org/post/{id}")
    R<Void> update(@PathVariable Long id, @RequestBody UpdatePostCommand command);

    @DeleteMapping("/org/post/{id}")
    R<Void> delete(@PathVariable Long id);
}
```

### 5.2 Controller

```java
@RestController
public class PostController implements PostApi {

    private final IPostService postService;

    public PostController(IPostService postService) {
        this.postService = postService;
    }

    @Override
    public R<PageResult<PostVO>> page(PostPageQuery query) {
        return R.ok(postService.page(query));
    }

    @Override
    public R<PostVO> get(Long id) {
        return R.ok(postService.get(id));
    }

    @Override
    public R<Void> create(CreatePostCommand command) {
        postService.create(command);
        return R.ok();
    }
}
```

### 5.3 错误示例

```java
// ❌ 禁止：直接暴露持久化对象
R<SysTenantEntity> get(Long id);

// ❌ 禁止：仓内业务 API 用 DTO
R<UserDTO> getUser(Long id);

// ❌ 禁止：写接口直接收 PO/Entity
R<Void> create(@RequestBody SysConfigPo po);

// ❌ 禁止：分页返回实体
R<PageResult<SysRoleEntity>> page(RolePageQuery query);
```

---

## 6. 统一返回约束

基线契约：

```java
R<Void>
R<XxxVO>
R<List<XxxVO>>
R<PageResult<XxxVO>>
```

规则：

- `R<T>` 中的 `T` 必须表达明确业务语义
- 禁止把 `Object`、裸 `Map` 作为通用返回模型
- 临时聚合结构如果确有必要，也应先定义成专用 `VO`

---

## 7. 兼容性策略

历史代码中如果仍存在 `PO` / `DTO` 暴露到 API 层，处理顺序如下：

1. 新增接口严格按本规范执行
2. 老接口优先把返回对象改成 `VO`
3. 老接口写入对象逐步改成 `Command`
4. 迁移期间允许 service / convert 层做兼容转换
5. 不允许新增新的 `DTO` 用法继续扩大范围

---

## 8. 检查清单

新增或修改 API 前，至少自查以下问题：

- 入参是不是 `Command` / `Query`，而不是 `PO` / `DTO` / `Entity`？
- 返回是不是 `VO`，而不是 `PO` / `DTO` / `Entity`？
- 分页是不是 `PageResult<XxxVO>`？
- Controller 是否只做协议适配，而不是业务拼装？
- 是否引入了新的 `Map<String, Object>` 式弱类型接口？
