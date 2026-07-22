package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.org.core.entity.PostEntity;
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
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OrgPostResourceHandlerIntegrationTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:org_post_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;"
                + "DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.autoconfigure.exclude=io.mango.org.starter.MangoOrgAutoConfiguration",
        "spring.flyway.enabled=false",
        "mango.persistence.flyway.enabled=false",
        "mango.persistence.schema-validation.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class OrgPostResourceHandlerIntegrationTest {

    private static final long DECLARATION_TENANT_ID = 2L;
    private static final String POST_CODE = "IT_621_COMPANY_A_INSTITUTION_ADMIN";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private SysOrgMapper sysOrgMapper;

    @Autowired
    private OrgPostResourceHandler handler;

    private final ResourceHandlerInvoker invoker = new ResourceHandlerInvoker();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists org_post");
        jdbcTemplate.execute("drop table if exists sys_org");
        jdbcTemplate.execute("""
                create table sys_org (
                    id bigint primary key,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    pid bigint not null default 0,
                    org_name varchar(100) not null,
                    org_code varchar(50) not null,
                    org_type int not null,
                    org_sort int not null default 0,
                    org_status char(1) not null default '1',
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    constraint uk_sys_org_tenant_code unique (tenant_id, org_code)
                )
                """);
        jdbcTemplate.execute("""
                create table org_post (
                    id bigint primary key,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    post_name varchar(100) not null,
                    post_code varchar(50) not null,
                    post_sort int not null default 0,
                    post_status char(1) not null default '1',
                    remark varchar(500),
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    constraint uk_org_post_tenant_code unique (tenant_id, post_code)
                )
                """);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void upsert_ambientTenantDiffersAndBusinessKeyExists_reusesExistingIdAndRestoresContext() {
        seedPost(301L, DECLARATION_TENANT_ID, POST_CODE, "旧岗位名");

        ResourceSyncResult result = invoker.upsert(handler,
                declaration("it-621-existing", 9101L, POST_CODE, "机构管理员"));

        assertThat(result.getTargetId()).isEqualTo(301L);
        assertThat(postRows(DECLARATION_TENANT_ID, POST_CODE)).containsExactly("301:机构管理员");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void tenantLine_realMapperQuery_filtersRowsByAmbientTenant() {
        seedPost(201L, 1L, "IT_621_TENANT_ONE", "租户一岗位");
        seedPost(202L, DECLARATION_TENANT_ID, "IT_621_TENANT_TWO", "租户二岗位");

        List<PostEntity> rows = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
                .orderByAsc(PostEntity::getId));

        assertThat(rows).extracting(PostEntity::getId).containsExactly(201L);
        assertThat(rows).extracting(PostEntity::getTenantId).containsOnly("1");
    }

    @Test
    void upsert_targetIdBelongsToDifferentBusinessKey_failsWithoutMutationAndRestoresContext() {
        seedPost(401L, DECLARATION_TENANT_ID, "IT_621_OTHER_POST", "其它岗位");

        assertThatThrownBy(() -> invoker.upsert(handler,
                declaration("it-621-target-conflict", 401L, POST_CODE, "机构管理员")))
                .hasMessageContaining("targetId")
                .hasMessageContaining("不匹配");

        assertThat(postRows(DECLARATION_TENANT_ID, "IT_621_OTHER_POST"))
                .containsExactly("401:其它岗位");
        assertThat(postRows(DECLARATION_TENANT_ID, POST_CODE)).isEmpty();
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void upsert_concurrentFirstInsert_convergesToOneRowAndOneTargetId() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<InvocationResult> first = executor.submit(() -> concurrentUpsert(
                    declaration("it-621-concurrent-a", 501L, POST_CODE, "机构管理员"), ready, start));
            Future<InvocationResult> second = executor.submit(() -> concurrentUpsert(
                    declaration("it-621-concurrent-b", 502L, POST_CODE, "机构管理员"), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            InvocationResult firstResult = first.get(10, TimeUnit.SECONDS);
            InvocationResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.targetId()).isEqualTo(secondResult.targetId());
            assertThat(firstResult.restoredTenantId()).isEqualTo("1");
            assertThat(secondResult.restoredTenantId()).isEqualTo("1");
            assertThat(postRows(DECLARATION_TENANT_ID, POST_CODE)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void provisionThenResourceSync_reusesProvisionedPost() {
        TenantProvisionCommand command = tenantCommand();
        provisionInDeclaredTenant(command);
        Long provisionedId = postId(DECLARATION_TENANT_ID, POST_CODE);

        ResourceSyncResult result = invoker.upsert(handler,
                declaration("it-621-provision-first", 601L, POST_CODE, "机构管理员"));

        assertThat(result.getTargetId()).isEqualTo(provisionedId);
        assertThat(postRows(DECLARATION_TENANT_ID, POST_CODE)).hasSize(1);
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void resourceSyncThenProvision_keepsResourcePostAsProvisionWinner() {
        ResourceSyncResult resourceResult = invoker.upsert(handler,
                declaration("it-621-resource-first", 701L, POST_CODE, "机构管理员"));

        provisionInDeclaredTenant(tenantCommand());

        assertThat(resourceResult.getTargetId()).isEqualTo(701L);
        assertThat(postId(DECLARATION_TENANT_ID, POST_CODE)).isEqualTo(701L);
        assertThat(postRows(DECLARATION_TENANT_ID, POST_CODE)).hasSize(1);
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    private InvocationResult concurrentUpsert(ResourceDeclaration declaration,
                                              CountDownLatch ready,
                                              CountDownLatch start) throws InterruptedException {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent resource upsert did not start in time");
        }
        ResourceSyncResult result = invoker.upsert(handler, declaration);
        return new InvocationResult(result.getTargetId(), MangoContextHolder.tenantId());
    }

    private void provisionInDeclaredTenant(TenantProvisionCommand command) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(String.valueOf(DECLARATION_TENANT_ID)));
            new OrgTenantProvisioner(sysOrgMapper, postMapper).provision(command);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private TenantProvisionCommand tenantCommand() {
        return new TenantProvisionCommand(DECLARATION_TENANT_ID, "it_621_company_a", "IT 621 A公司");
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
        put(declaration, "tenantId", ResourceFieldType.LONG, DECLARATION_TENANT_ID);
        put(declaration, "postCode", ResourceFieldType.STRING, postCode);
        put(declaration, "postName", ResourceFieldType.STRING, postName);
        put(declaration, "sort", ResourceFieldType.INT, 1);
        put(declaration, "status", ResourceFieldType.STRING, "1");
        put(declaration, "remark", ResourceFieldType.STRING, "IT 621 resource sync");
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private void seedPost(long id, long tenantId, String postCode, String postName) {
        jdbcTemplate.update("""
                        insert into org_post
                            (id, tenant_id, post_name, post_code, post_sort, post_status, created_at, updated_at)
                        values (?, ?, ?, ?, 1, '1', current_timestamp, current_timestamp)
                        """,
                id, String.valueOf(tenantId), postName, postCode);
    }

    private List<String> postRows(long tenantId, String postCode) {
        return jdbcTemplate.query("""
                        select id, post_name
                        from org_post
                        where tenant_id = ? and post_code = ?
                        order by id
                        """,
                (resultSet, rowNum) -> resultSet.getLong("id") + ":" + resultSet.getString("post_name"),
                String.valueOf(tenantId), postCode);
    }

    private Long postId(long tenantId, String postCode) {
        return jdbcTemplate.queryForObject("""
                select id from org_post where tenant_id = ? and post_code = ?
                """, Long.class, String.valueOf(tenantId), postCode);
    }

    private record InvocationResult(Long targetId, String restoredTenantId) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = {PostMapper.class, SysOrgMapper.class})
    @Import(OrgPostResourceHandler.class)
    static class TestApplication {
    }
}
