# Mango Generator

代码生成器模块，包含用于生成 Mango 脚手架模块的 Velocity 模板。

## 模板目录

```
templates/
└── module/
    └── pom.xml.vm          # 模块 pom.xml 模板
```

## 使用方式

通过 `mango-maven-plugin` 的 `gen-module` 命令生成新模块：

```bash
mvn mango:gen-module -Dname=user
```

生成结构：

```
mango-user/
├── pom.xml
├── mango-user-api/
├── mango-user-core/
├── mango-user-starter/
└── mango-user-starter-remote/
```

## 模板变量

| 变量 | 说明 |
|------|------|
| `${moduleName}` | 模块名称（小写） |
| `${ModuleName}` | 模块名称（首字母大写） |
