# Mango Evaluator

代码质量评估模块，提供四维评估标准和结构化评估报告。

## 四维评估标准

| 维度 | 权重 | 最低阈值 | 评估内容 |
|------|------|----------|----------|
| Design Quality | 30% | ≥7/10 | 模块结构、类设计、依赖关系 |
| Originality | 20% | ≥6/10 | 代码复用、设计模式应用 |
| Craft | 25% | ≥7/10 | 命名规范、代码整洁度、重复度 |
| Functionality | 25% | ≥8/10 | 功能完整性、测试覆盖、边界处理 |

## 评估器

| 评估器 | 支持类型 | 说明 |
|--------|----------|------|
| CodeEvaluator | code | Java 代码质量评估 |
| PRDEvaluator | prd | 产品需求文档评估 |

## 使用方式

```java
// 代码评估
ArtifactEvaluator codeEvaluator = new CodeEvaluator();
EvaluationReport report = codeEvaluator.evaluate("/path/to/src");

// PRD 评估
ArtifactEvaluator prdEvaluator = new PRDEvaluator();
EvaluationReport report = prdEvaluator.evaluate("/path/to/prd");

// 检查是否通过
if (report.isPassed()) {
    System.out.println("Passed with score: " + report.getScore());
} else {
    System.out.println("Failed with issues: " + report.getIssues().size());
}

// 获取维度评分
Dimensions dims = report.getDimensions();
System.out.println("Design: " + dims.getDesignQuality());
System.out.println("Craft: " + dims.getCraft());
```

## Maven 集成

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-evaluator</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## CLI 使用

```bash
# 评估代码
mvn mango:evaluate -Dartifact=code -DbaseDir=/path/to/src

# 评估 PRD
mvn mango:evaluate -Dartifact=prd -DbaseDir=/path/to/prd

# JSON 输出
mvn mango:evaluate -Dartifact=code -Doutput=json

# 保存报告
mvn mango:evaluate -Dartifact=code -DreportFile=target/evaluation-report.json
```
