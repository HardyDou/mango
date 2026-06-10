package io.mango.infra.persistence.starter.tenant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.api.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TenantIsolationIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mango_tenant_isolation;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.schema-validation.enabled=false",
                "mybatis-plus.mapper-locations=classpath:/mapper/tenant/*.xml"
        }
)
class TenantIsolationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TenantDemoMapper tenantDemoMapper;

    @Autowired
    private TenantDemoXmlMapper tenantDemoXmlMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tenant_demo");
        jdbcTemplate.execute("""
                CREATE TABLE tenant_demo (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(64),
                    status INT,
                    tenant_id VARCHAR(64),
                    created_by BIGINT,
                    created_at TIMESTAMP,
                    updated_by BIGINT,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO tenant_demo (id, name, status, tenant_id, created_by, created_at, updated_by, updated_at)
                VALUES
                  (1001, 'alpha', 1, 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (1002, 'beta', 1, 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (2001, 'alpha-other', 1, 'tenant-b', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
                """);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(9001L, "tenant-a", "tester", "default", "USER", "USER", 9001L, "test"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void baseMapperSelect_shouldApplyTenantScopeWithoutBusinessTenantCondition() {
        List<TenantDemoEntity> rows = tenantDemoMapper.selectList(new LambdaQueryWrapper<TenantDemoEntity>()
                .eq(TenantDemoEntity::getStatus, 1)
                .orderByAsc(TenantDemoEntity::getId));

        assertThat(rows)
                .extracting(TenantDemoEntity::getId)
                .containsExactly(1001L, 1002L);
        assertThat(rows)
                .extracting(TenantDemoEntity::getTenantId)
                .containsOnly("tenant-a");
    }

    @Test
    void wrapperSelect_shouldApplyTenantScopeWithoutManualTenantPredicate() {
        TenantDemoEntity row = tenantDemoMapper.selectOne(new LambdaQueryWrapper<TenantDemoEntity>()
                .eq(TenantDemoEntity::getName, "alpha"));

        assertThat(row).isNotNull();
        assertThat(row.getId()).isEqualTo(1001L);
        assertThat(row.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void baseMapperInsert_shouldFillTenantAndAuditFieldsFromContext() {
        TenantDemoEntity entity = new TenantDemoEntity();
        entity.setId(3001L);
        entity.setName("created-by-context");
        entity.setStatus(1);

        assertThat(tenantDemoMapper.insert(entity)).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT tenant_id, created_by, created_at, updated_by, updated_at
                FROM tenant_demo
                WHERE id = 3001
                """);
        assertThat(row.get("TENANT_ID")).isEqualTo("tenant-a");
        assertThat(row.get("CREATED_BY")).isEqualTo(9001L);
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_BY")).isEqualTo(9001L);
        assertThat(row.get("UPDATED_AT")).isNotNull();
    }

    @Test
    void customMapperSql_shouldApplyTenantScopeWithoutManualTenantPredicate() {
        List<TenantDemoVO> rows = tenantDemoXmlMapper.selectByStatus(1);

        assertThat(rows)
                .extracting(TenantDemoVO::getId)
                .containsExactly(1001L, 1002L);
        assertThat(rows)
                .extracting(TenantDemoVO::getTenantId)
                .containsOnly("tenant-a");
    }

    @Test
    void customMapperPage_shouldApplyTenantScopeAndPaginationWithoutManualTenantPredicate() {
        IPage<TenantDemoVO> page = tenantDemoXmlMapper.pageByStatus(new Page<>(1, 1), 1);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords())
                .extracting(TenantDemoVO::getId)
                .containsExactly(1001L);
        assertThat(page.getRecords())
                .extracting(TenantDemoVO::getTenantId)
                .containsOnly("tenant-a");
    }

    @SpringBootApplication
    @MapperScan(basePackageClasses = TenantDemoMapper.class, annotationClass = Mapper.class)
    static class TestApplication {
    }

    @Mapper
    interface TenantDemoMapper extends BaseMapper<TenantDemoEntity> {
    }

    @Mapper
    interface TenantDemoXmlMapper {

        List<TenantDemoVO> selectByStatus(@Param("status") Integer status);

        IPage<TenantDemoVO> pageByStatus(Page<TenantDemoVO> page, @Param("status") Integer status);
    }

    @TableName("tenant_demo")
    static class TenantDemoEntity extends TenantEntity {

        private String name;

        private Integer status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    static class TenantDemoVO {

        private Long id;

        private String name;

        private Integer status;

        private String tenantId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
