package io.mango.infra.persistence.starter.scope;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MybatisPlusDataScopeApplier 测试")
class MybatisPlusDataScopeApplierTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("apply should add self condition")
    void apply_self_addsSelfCondition() {
        setContext();
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.SELF, Set.of(), true)));
        QueryWrapper<Object> wrapper = new QueryWrapper<>();

        applier.apply(wrapper, "payment:order:list", DataScopeMapping.builder().selfField("created_by").build());

        assertTrue(wrapper.getSqlSegment().contains("created_by"));
    }

    @Test
    @DisplayName("apply should add org and self condition")
    void apply_orgWithSelf_addsOrgAndSelfCondition() {
        setContext();
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.ORG, Set.of("10", "20"), true)));
        QueryWrapper<Object> wrapper = new QueryWrapper<>();

        applier.apply(wrapper, "payment:order:list",
                DataScopeMapping.builder().orgField("org_id").selfField("created_by").build());

        String segment = wrapper.getSqlSegment();
        assertTrue(segment.contains("org_id"));
        assertTrue(segment.contains("created_by"));
        assertTrue(segment.contains("OR"));
    }

    @Test
    @DisplayName("apply should pass when mapped table has required columns")
    void apply_withTableColumns_passes() throws Exception {
        setContext();
        DataSource dataSource = dataSource("data_scope_columns_pass");
        execute(dataSource, """
                CREATE TABLE payment_order (
                    id BIGINT PRIMARY KEY,
                    created_by BIGINT,
                    org_id BIGINT
                )
                """);
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.ORG, Set.of("10"), true)),
                Optional.of(dataSource));
        QueryWrapper<Object> wrapper = new QueryWrapper<>();

        applier.apply(wrapper, "payment:order:list", DataScopeMapping.builder()
                .tableName("payment_order")
                .orgField("org_id")
                .selfField("created_by")
                .build());

        String segment = wrapper.getSqlSegment();
        assertTrue(segment.contains("org_id"));
        assertTrue(segment.contains("created_by"));
    }

    @Test
    @DisplayName("apply should fail fast when mapped table misses required column")
    void apply_withMissingMappedColumn_throws() throws Exception {
        setContext();
        DataSource dataSource = dataSource("data_scope_columns_fail");
        execute(dataSource, """
                CREATE TABLE payment_order (
                    id BIGINT PRIMARY KEY,
                    created_by BIGINT
                )
                """);
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.ORG, Set.of("10"), false)),
                Optional.of(dataSource));

        assertThatThrownBy(() -> applier.apply(new QueryWrapper<>(), "payment:order:list", DataScopeMapping.builder()
                .tableName("payment_order")
                .orgField("org_id")
                .selfField("created_by")
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("org_id")
                .hasMessageContaining("payment_order");
    }

    @Test
    @DisplayName("apply should not require mapped fields when org scope has no values")
    void apply_orgWithoutValues_addsDenyCondition() throws Exception {
        setContext();
        DataSource dataSource = dataSource("data_scope_no_values");
        execute(dataSource, """
                CREATE TABLE payment_order (
                    id BIGINT PRIMARY KEY
                )
                """);
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.ORG, Set.of(), false)),
                Optional.of(dataSource));
        QueryWrapper<Object> wrapper = new QueryWrapper<>();

        applier.apply(wrapper, "payment:order:list", DataScopeMapping.builder()
                .tableName("payment_order")
                .build());

        assertTrue(wrapper.getSqlSegment().contains("1 = 0"));
    }

    @Test
    @DisplayName("apply should fail fast when self field is missing")
    void apply_missingSelfField_throws() {
        setContext();
        MybatisPlusDataScopeApplier applier = new MybatisPlusDataScopeApplier(
                resourceCode -> Optional.of(new DataScopeRule(DataScopeRule.Mode.SELF, Set.of(), true)));

        assertThrows(IllegalArgumentException.class,
                () -> applier.apply(new QueryWrapper<>(), "payment:order:list", DataScopeMapping.builder().build()));
    }

    private void setContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L,
                1001L,
                "1",
                "admin",
                "INTERNAL",
                "INTERNAL_USER",
                "INTERNAL_ORG",
                1L,
                "internal-admin"));
    }

    private DataSource dataSource(String name) {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void execute(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
