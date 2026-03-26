# 模块分层规范 (module-rules)

## 1. 服务模块分层

### 1.1 模块命名规则

每个服务必须包含以下 4 个模块：

| 后缀 | 说明 | 示例 |
|------|------|------|
| `-api` | 接口定义 | `mango-order-api` |
| `-core` | 核心业务实现 | `mango-order-core` |
| `-starter` | 本地调用启动器 | `mango-order-starter` |
| `-starter-remote` | 远程调用启动器 | `mango-order-starter-remote` |

### 1.2 目录结构

```
mango-order/
├── mango-order-api/                    # 接口定义
│   ├── src/main/java/
│   │   └── com/mango/order/api/
│   │       ├── OrderService.java        # 服务接口
│   │       └── dto/
│   │           ├── OrderDTO.java
│   │           └── CreateOrderRequest.java
│   └── pom.xml
│
├── mango-order-core/                   # 核心业务实现
│   ├── src/main/java/
│   │   └── com/mango/order/
│   │       ├── service/
│   │       │   └── OrderServiceImpl.java
│   │       └── mapper/
│   │           └── OrderMapper.java
│   └── pom.xml
│
├── mango-order-starter/               # 本地调用启动器
│   ├── src/main/java/
│   │   └── com/mango/order/starter/
│   │       └── OrderAutoConfiguration.java
│   └── pom.xml
│
└── mango-order-starter-remote/        # 远程调用启动器
    ├── src/main/java/
    │   └── com/mango/order/starter/
    │       └── OrderRemoteAutoConfiguration.java
    └── pom.xml
```

---

## 2. Core 依赖原则

### 2.1 依赖规则

**Core 只依赖其他服务的 API，不依赖任何 Provider/Starter**

```xml
<!-- ✅ 正确：只依赖 API -->
<dependency>
    <groupId>com.mango</groupId>
    <artifactId>mango-inventory-api</artifactId>
</dependency>

<!-- ❌ 错误：依赖 Starter -->
<dependency>
    <groupId>com.mango</groupId>
    <artifactId>mango-inventory-starter</artifactId>
</dependency>
```

### 2.2 依赖图示

```
┌─────────────────────────────────────────────────────────────┐
│  Core 只依赖其他服务的 API，不依赖任何 Provider/Starter           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  App 模块的 pom.xml 决定：依赖 starter 还是 starter-remote     │
│                                                              │
│  改 App 的 pom.xml 依赖，不动 Core 代码                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 调用透明性

### 3.1 Core 层调用

```java
// OrderServiceImpl.java
@Service
public class OrderServiceImpl implements OrderService {

    // 只注入接口，不知道是本地还是远程
    @Autowired
    private InventoryService inventoryService;  // 接口

    @Autowired
    private PaymentService paymentService;  // 接口

    @Override
    public void createOrder(OrderDTO dto) {
        // 业务逻辑
        inventoryService.deduct(productId, quantity);  // 透明调用
        paymentService.pay(orderId, amount);  // 透明调用
    }
}
```

### 3.2 运行时注入

```
运行时：Spring Boot 自动配置注入实现

→ 依赖 mango-inventory-starter → 注入 LocalInventoryService
→ 依赖 mango-inventory-starter-remote → 注入 InventoryRemoteClient
```

---

## 4. 部署规则

### 4.1 App 依赖示例

```xml
<!-- 单体部署：所有本地调用 -->
<dependency>mango-order-core</dependency>
<dependency>mango-inventory-starter</dependency>  <!-- 本地 -->
<dependency>mango-payment-starter</dependency>    <!-- 本地 -->

<!-- 独立部署：其他服务远程调用 -->
<dependency>mango-order-core</dependency>
<dependency>mango-inventory-starter-remote</dependency>  <!-- 远程 -->
<dependency>mango-payment-starter-remote</dependency>    <!-- 远程 -->
```

### 4.2 部署场景

| 场景 | App | 依赖 |
|------|-----|------|
| 单体 | mango-app-all | 所有 core + 所有 starter |
| 订单独立 | mango-app-order | order-core + inventory-starter-remote + payment-starter-remote |
| 库存独立 | mango-app-inventory | inventory-core + order-starter-remote + payment-starter-remote |
| 支付独立 | mango-app-payment | payment-core + order-starter-remote + inventory-starter-remote |

---

## 5. SPI 机制

### 5.1 SPI 配置

```java
// resources/META-INF/services/
com.mango.inventory.api.InventoryService
```

文件内容：
```
# 本地实现
com.mango.inventory.starter.LocalInventoryService

# 远程实现
com.mango.inventory.starter.remote.InventoryRemoteClient
```

### 5.2 自动配置

```java
// OrderAutoConfiguration.java
@Configuration
@ConditionalOnProperty(name = "mango.order.mode", havingValue = "local")
public class OrderAutoConfiguration {

    @Bean
    @Primary
    public InventoryService inventoryService() {
        return new LocalInventoryService();
    }
}
```

---

## 6. 模块开发流程

### 6.1 新建服务步骤

1. 创建 `mango-xxx-api` - 定义接口和 DTO
2. 创建 `mango-xxx-core` - 实现业务逻辑
3. 创建 `mango-xxx-starter` - 本地调用配置
4. 创建 `mango-xxx-starter-remote` - 远程调用配置

### 6.2 消费者使用

```xml
<!-- 切换为远程调用 -->
<dependency>
    <groupId>com.mango</groupId>
    <artifactId>mango-xxx-starter-remote</artifactId>
</dependency>
```
