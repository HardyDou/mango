package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.org.core.service.impl.OrgTenantProvisioner;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.execution.ResourceHandlerInvoker;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import io.mango.system.api.tenant.TenantProvisionCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OrgPostResourceHandlerMySqlIntegrationTest.TestApplication.class)
@EnabledIfEnvironmentVariable(named = "MANGO_MYSQL_IT_URL", matches = ".+")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=io.mango.org.starter.MangoOrgAutoConfiguration",
        "spring.flyway.enabled=false",
        "mango.persistence.flyway.enabled=false",
        "mango.persistence.schema-validation.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class OrgPostResourceHandlerMySqlIntegrationTest {

    private static final long TENANT_ID = 9620621L;
    private static final String TENANT_CODE = "IT621MYSQL";
    private static final String POST_CODE = TENANT_CODE + "_INSTITUTION_ADMIN";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private SysOrgMapper sysOrgMapper;

    @Autowired
    private OrgPostResourceHandler handler;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ResourceHandlerInvoker invoker = new ResourceHandlerInvoker();

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MANGO_MYSQL_IT_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("MANGO_MYSQL_IT_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("MANGO_MYSQL_IT_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void setUp() {
        cleanup();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
        cleanup();
    }

    @Test
    void existingPostInDeclaredTenantIsUpdatedAndAmbientTenantIsRestored() {
        seedPost(962062100000000001L, POST_CODE, "旧岗位名");

        ResourceSyncResult result = invoker.upsert(handler,
                declaration("962062100000001001", 962062100000001001L, POST_CODE, "机构管理员"));

        assertThat(result.getTargetId()).isEqualTo(962062100000000001L);
        assertThat(postRows(POST_CODE)).containsExactly("962062100000000001:机构管理员");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void wrongTargetIdFailsWithoutChangingExistingRow() {
        seedPost(962062100000000002L, TENANT_CODE + "_OTHER", "其它岗位");

        assertThatThrownBy(() -> invoker.upsert(handler,
                declaration("962062100000001002", 962062100000000002L, POST_CODE, "机构管理员")))
                .hasMessageContaining("targetId")
                .hasMessageContaining("不匹配");

        assertThat(postRows(TENANT_CODE + "_OTHER"))
                .containsExactly("962062100000000002:其它岗位");
        assertThat(postRows(POST_CODE)).isEmpty();
    }

    @Test
    void concurrentResourceFirstInsertConvergesToOneMysqlRowAndId() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Long> first = executor.submit(() -> concurrentUpsert(
                    declaration("962062100000001003", 962062100000000003L, POST_CODE, "机构管理员"),
                    ready, start));
            Future<Long> second = executor.submit(() -> concurrentUpsert(
                    declaration("962062100000001004", 962062100000000004L, POST_CODE, "机构管理员"),
                    ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
            assertThat(postRows(POST_CODE)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void repeatableReadProvisionSnapshotSeesResourceWinnerThroughLockingRead() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch snapshotEstablished = new CountDownLatch(1);
        CountDownLatch resourceCommitted = new CountDownLatch(1);
        try {
            Future<?> provision = executor.submit(() -> inTenantTransaction(() -> {
                postMapper.selectCount(new LambdaQueryWrapper<PostEntity>()
                        .eq(PostEntity::getTenantId, TENANT_ID)
                        .eq(PostEntity::getPostCode, POST_CODE));
                snapshotEstablished.countDown();
                await(resourceCommitted);
                provisioner().provision(tenantCommand());
            }));
            assertThat(snapshotEstablished.await(5, TimeUnit.SECONDS)).isTrue();

            ResourceSyncResult resource = invoker.upsert(handler,
                    declaration("962062100000001005", 962062100000000005L, POST_CODE, "机构管理员"));
            resourceCommitted.countDown();
            provision.get(10, TimeUnit.SECONDS);

            assertThat(resource.getTargetId()).isEqualTo(962062100000000005L);
            assertThat(postRows(POST_CODE)).containsExactly("962062100000000005:机构管理员");
        } finally {
            resourceCommitted.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentProvisioningConvergesRootAndDefaultPosts() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> concurrentProvision(ready, start));
            Future<?> second = executor.submit(() -> concurrentProvision(ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);

            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from sys_org where tenant_id = ? and org_code = ?",
                    Long.class, String.valueOf(TENANT_ID), TENANT_CODE + "_ROOT")).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from org_post where tenant_id = ? and post_code like ?",
                    Long.class, String.valueOf(TENANT_ID), TENANT_CODE + "_%")).isEqualTo(3L);
        } finally {
            executor.shutdownNow();
        }
    }

    private Long concurrentUpsert(ResourceDeclaration declaration,
                                  CountDownLatch ready,
                                  CountDownLatch start) {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        try {
            ready.countDown();
            await(start);
            ResourceSyncResult result = invoker.upsert(handler, declaration);
            assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
            return result.getTargetId();
        } finally {
            MangoContextHolder.clear();
        }
    }

    private void concurrentProvision(CountDownLatch ready, CountDownLatch start) {
        inTenantTransaction(() -> {
            sysOrgMapper.selectCount(new LambdaQueryWrapper<SysOrgEntity>()
                    .eq(SysOrgEntity::getTenantId, TENANT_ID));
            ready.countDown();
            await(start);
            provisioner().provision(tenantCommand());
        });
    }

    private void inTenantTransaction(Runnable action) {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId(String.valueOf(TENANT_ID)));
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
        } finally {
            MangoContextHolder.clear();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("MySQL concurrency barrier timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MySQL concurrency barrier interrupted", exception);
        }
    }

    private OrgTenantProvisioner provisioner() {
        return new OrgTenantProvisioner(sysOrgMapper, postMapper);
    }

    private TenantProvisionCommand tenantCommand() {
        return new TenantProvisionCommand(TENANT_ID, TENANT_CODE.toLowerCase(), "MySQL 621 验收租户");
    }

    private ResourceDeclaration declaration(String id, Long targetId, String postCode, String postName) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.ORG_POST);
        declaration.setModuleCode("org");
        declaration.setBizKey("org.post." + id);
        declaration.setName(postName);
        declaration.setTargetModule("org");
        declaration.setFields(new LinkedHashMap<>());
        put(declaration, "targetId", ResourceFieldType.LONG, targetId);
        put(declaration, "tenantId", ResourceFieldType.LONG, TENANT_ID);
        put(declaration, "postCode", ResourceFieldType.STRING, postCode);
        put(declaration, "postName", ResourceFieldType.STRING, postName);
        put(declaration, "sort", ResourceFieldType.INT, 1);
        put(declaration, "status", ResourceFieldType.STRING, "1");
        put(declaration, "remark", ResourceFieldType.STRING, "MySQL 8.4 integration test");
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private void seedPost(long id, String postCode, String postName) {
        jdbcTemplate.update("""
                        insert into org_post
                            (id, tenant_id, post_name, post_code, post_sort, post_status, created_at, updated_at)
                        values (?, ?, ?, ?, 1, '1', current_timestamp, current_timestamp)
                        """,
                id, String.valueOf(TENANT_ID), postName, postCode);
    }

    private List<String> postRows(String postCode) {
        return jdbcTemplate.query("""
                        select id, post_name from org_post
                        where tenant_id = ? and post_code = ? order by id
                        """,
                (resultSet, rowNum) -> resultSet.getLong("id") + ":" + resultSet.getString("post_name"),
                String.valueOf(TENANT_ID), postCode);
    }

    private void cleanup() {
        if (jdbcTemplate == null) {
            return;
        }
        jdbcTemplate.update("delete from org_post where tenant_id = ?", String.valueOf(TENANT_ID));
        jdbcTemplate.update("delete from sys_org where tenant_id = ?", String.valueOf(TENANT_ID));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = {PostMapper.class, SysOrgMapper.class})
    @Import(OrgPostResourceHandler.class)
    static class TestApplication {
    }
}
