# 后端开发规范 (backend-development-rules)

## 1. 开发完成标准

后端功能开发完成后，必须满足以下条件才能交付：

| 条件 | 说明 |
|------|------|
| 功能代码 | 核心业务逻辑实现完成 |
| 单元测试 | Service/Mapper 层覆盖率 ≥ 80% |
| 集成测试 | 端到端 API 测试通过 |
| 接口文档 | REST API 接口契约文档完整 |

### 1.1 禁止事项

- **禁止**未经测试的接口交付给前端
- **禁止**跳过测试直接提交代码
- **禁止**只有 mock 测试而无真实数据库集成测试

---

## 2. 测试要求

### 2.1 单元测试 (必须)

每个后端模块必须包含单元测试：

```java
// 目录结构
src/test/java/io/mango/{module}/
├── service/           # Service 层测试
│   └── {Entity}ServiceTest.java
├── controller/        # Controller 层测试
│   └── {Entity}ControllerTest.java
└── mapper/            # Mapper 层测试
    └── {Entity}MapperTest.java
```

**Service 层测试示例：**
```java
@SpringBootTest
class SysUserServiceTest {

    @Autowired
    private SysUserService sysUserService;

    @Test
    void findById_existingId_returnsUser() {
        SysUser user = sysUserService.findById(1L);
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }

    @Test
    void save_validUser_returnsSavedUser() {
        SysUser user = new SysUser();
        user.setUsername("test");
        user.setPassword("password");
        SysUser saved = sysUserService.save(user);
        assertNotNull(saved.getId());
    }

    @Test
    void save_nullUsername_throwsValidationException() {
        SysUser user = new SysUser();
        assertThrows(ValidationException.class, () -> sysUserService.save(user));
    }
}
```

### 2.2 集成测试 (必须)

每个 API 接口必须包含集成测试，验证真实数据库交互：

```java
// 目录结构
src/test/java/io/mango/{module}/
└── integration/
    └── {Entity}ApiIntegrationTest.java
```

**集成测试示例：**
```java
@SpringBootTest
@AutoConfigureMockMvc
class SysAreaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysAreaMapper sysAreaMapper;

    @Test
    void getTree_rootRequest_returnsProvinceList() throws Exception {
        mockMvc.perform(get("/mango/area/tree")
                .header("Authorization", "Bearer " + getTestToken())
                .param("parentId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getTree_withValidParentId_returnsChildren() throws Exception {
        // 验证懒加载：只返回指定父节点的直接子节点
        mockMvc.perform(get("/mango/area/tree")
                .header("Authorization", "Bearer " + getTestToken())
                .param("parentId", "440000")) // 广东省
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].parentId").value(440000));
    }

    @Test
    void getTree_invalidParentId_returnsEmpty() throws Exception {
        mockMvc.perform(get("/mango/area/tree")
                .header("Authorization", "Bearer " + getTestToken())
                .param("parentId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
```

### 2.3 交付前端前的接口测试

提供给前端的 **每个** 接口必须通过以下测试：

| 测试类型 | 测试内容 | 通过标准 |
|----------|----------|----------|
| 正常请求 | 有效参数调用接口 | 返回预期数据 |
| 参数校验 | 空值、非法值、超长字符串 | 返回明确错误信息 |
| 权限验证 | 无 token、过期 token、错误 token | 返回 401/403 |
| 租户隔离 | A 租户不能访问 B 租户数据 | 返回空或 403 |
| 性能验证 | 响应时间 < 500ms | 超时返回错误 |

**接口测试报告格式：**
```markdown
## 接口测试报告: /mango/area/tree

### 测试结果: ✅ 通过

| 测试项 | 输入 | 预期 | 实际 | 状态 |
|--------|------|------|------|------|
| 根节点查询 | parentId=0 | 返回省列表 | 返回 32 个省 | ✅ |
| 子节点懒加载 | parentId=440000 | 只返回广东下属 | 返回 21 个市 | ✅ |
| 空父节点 | parentId=999999 | 返回空数组 | 返回 [] | ✅ |
| 无权限 | 无 Authorization | 返回 401 | 返回 401 | ✅ |

### 凭证
- 测试命令: `mvn test -Dtest=SysAreaApiIntegrationTest`
- 覆盖率: Service 95%, Controller 85%
```

---

## 3. 开发流程

### 3.1 后端开发步骤

```
1. 理解接口契约 (来自 frontend-backend-chat.md 或 Plan)
   ↓
2. 实现功能代码 (Controller → Service → Mapper)
   ↓
3. 编写单元测试 (Service 层 Mock 测试)
   ↓
4. 编写集成测试 (真实 API 调用测试)
   ↓
5. 运行完整测试套件
   mvn test                    # 单元测试
   mvn verify                  # 集成测试
   ↓
6. 生成接口测试报告
   ↓
7. 前端 Handoff (附带测试报告)
```

### 3.2 禁止跳步

```
❌ 先交付，后补测试
❌ 只写单元测试，不写集成测试
❌ Mock 测试通过就交付 (未验证真实数据库交互)
❌ 前端催就跳过测试
```

---

## 4. 测试数据管理

### 4.1 测试数据来源

| 类型 | 来源 | 说明 |
|------|------|------|
| 单元测试 | Mock 对象 | 不依赖外部数据 |
| 集成测试 | Testcontainers 或 H2 | 真实数据库操作 |
| 冒烟测试 | 固定测试数据 | 可重复执行 |

### 4.2 集成测试数据库配置

```java
// 使用 H2 内存数据库进行集成测试
@ActiveProfiles("test")
@SpringBootTest

// application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:sql/V1__mango_area_init.sql
```

---

## 5. 代码检查

### 5.1 提交前必须通过

```bash
# 在模块目录执行
mvn clean test          # 单元测试
mvn verify              # 集成测试
mvn checkstyle:check    # 代码风格
mvn spotbugs:check      # Bug 检测
mvn pmd:check           # 代码分析
```

### 5.2 覆盖率要求

| 层级 | 最低覆盖率 |
|------|----------|
| Service 层 | ≥ 80% |
| Controller 层 | ≥ 70% |
| Mapper 层 | ≥ 60% |

---

## 6. 前端交付标准

### 6.1 交付清单

向后端交付接口时，必须提供：

- [ ] **接口测试报告** (本规范第 2.3 节格式)
- [ ] **API 契约文档** (路径、参数、响应格式)
- [ ] **测试账号/Token** (如有需要)
- [ ] **Mock 数据示例** (便于前端调试)

### 6.2 交付确认

前端接收接口前，应确认：

- [ ] 接口已在测试环境验证通过
- [ ] 接口文档与实际实现一致
- [ ] 错误码有明确说明
- [ ] 边界条件有明确描述

---

## 7. 违规处理

| 违规行为 | 处理方式 |
|----------|----------|
| 未经测试的接口交付 | Evaluator 质检不通过，打回重做 |
| 提交代码未通过测试 | CI/CD 流水线失败 |
| 覆盖率不达标 | 禁止合并代码 |
| 伪造测试结果 | 代码回滚，追究责任 |

---

## 8. 相关规范

| 规范 | 说明 |
|------|------|
| rules/test-rules.md | 测试覆盖率、边界条件测试 |
| rules/api-rules.md | RESTful API 设计规范 |
| rules/code-rules.md | 代码编写规范 |
| rules/security-rules.md | 安全规范 (权限、认证) |
