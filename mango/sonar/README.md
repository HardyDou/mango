# SonarQube + Alibaba P3C 集成

## 快速启动

```bash
cd sonar
docker compose up -d

# 等待 SonarQube 启动完成（约 2-3 分钟）
# 访问 http://localhost:9000
# 默认账号: admin / admin
```

## 安装 Alibaba P3C 插件（可选）

P3C 插件提供阿里巴巴 Java 编码规约检查：

1. 下载 `sonar-p3c-plugin`：https://github.com/alibaba/p3c/releases
2. 放入容器：
   ```bash
   docker cp /path/to/sonar-p3c-plugin.jar sonarqube:/opt/sonarqube/extensions/plugins/
   ```
3. 重启：
   ```bash
   docker compose restart sonarqube
   ```

## 运行代码扫描

### 方式一：使用扫描脚本

```bash
cd /Users/hardy/Work/company02/mango

# 扫描单个模块
./sonar/sonar-scan.sh scan mango-common

# 或直接使用 sonar-scanner（需先编译）
cd mango-common
mvn clean compile
sonar-scanner -Dsonar.token=$SONAR_TOKEN
```

### 方式二：Maven 方式

```bash
# 在项目根目录
mvn sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

## 扫描结果

最新扫描结果（mango-common）：

| 指标 | 值 | 状态 |
|------|-----|------|
| Bugs | 0 | ✅ |
| Vulnerabilities | 0 | ✅ |
| Code Smells | 10 | 🟡 Minor |
| Coverage | 0% | ❌ 需补充 |
| Duplicated Lines | 0% | ✅ |

查看完整报告：http://localhost:9000/dashboard?id=mango-common

## 配置说明

### 环境变量

复制 `.env.example` 为 `.env` 并填入你的 token：

```bash
cp .env.example .env
```

### Maven 配置

在 `mango-parent/pom.xml` 中已配置：

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.11.0</version>
</plugin>
```

### 项目配置

每个模块的 `sonar-project.properties` 示例：

```properties
sonar.projectKey=mango-common
sonar.projectName=Mango Common
sonar.sources=src/main/java
sonar.java.binaries=target/classes
sonar.host.url=http://localhost:9000
sonar.java.source=17
```

## 质量门禁标准

| 指标 | 阈值 |
|------|------|
| Bugs | 0 |
| Vulnerabilities | 0 |
| Coverage | ≥ 60% |
| Duplicated Lines | ≤ 3% |
| Code Smells | ≤ 50 (Major) |

## 常见问题

### Q: Token 如何获取？
A: 访问 http://localhost:9000 → Administration → Security → Users → Tokens → Generate

### Q: 扫描失败"compiled classes"？
A: 先执行 `mvn clean compile` 编译代码

### Q: SonarQube 启动缓慢？
A: 首次启动约需 2-3 分钟，等待 postgres 和 sonarqube 完全就绪
