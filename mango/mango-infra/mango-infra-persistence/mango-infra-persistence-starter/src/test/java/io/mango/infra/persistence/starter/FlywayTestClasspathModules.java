package io.mango.infra.persistence.starter;

import java.util.List;

/**
 * 测试 classpath 中的 Flyway 迁移模块清单。
 *
 * <p>新增 {@code src/test/resources/db/migration/<module>} 后必须同步本清单；否则
 * {@code PersistenceFlywayAutoConfiguration} 的“模块必须完整声明”校验会让所有未声明该模块的测试失败
 * （Issue #1226）。各测试类用本清单为不参与当前场景的模块补 {@code enabled=false} 和 {@code skip-reason}。
 */
final class FlywayTestClasspathModules {

    /**
     * 测试 classpath 中全部 Flyway 模块目录名。
     */
    static final List<String> ALL = List.of(
            "another-test",
            "business-upgrade",
            "cold-missing-snapshot",
            "comparison-data",
            "link",
            "payment",
            "persistence-test");

    private FlywayTestClasspathModules() {
    }
}
