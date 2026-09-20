package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.org.api.OrgReferenceProvider;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        OrgMemberBindingResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:org_member_binding_resource_handler;MODE=MySQL;"
                + "DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class OrgMemberBindingResourceHandlerIntegrationTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 1L;
    private static final long MEMBER_ID = 1001L;
    private static final long ORG_ID = 2001L;
    private static final long POST_ID = 3001L;
    private static final String ORG_CODE = "MANGO_GROUP";
    private static final String POST_CODE = "GROUP_ADMIN";
    private static final String TARGET_TABLE = "tenant_member_org";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrgMemberBindingResourceHandler handler;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    void upsert_sameDeclarationAcrossEmptyDatabases_persistsIdenticalStableRelation() {
        ResourceDeclaration declaration = resource();

        ResourceSyncResult firstResult = handler.upsert(declaration);
        RelationSnapshot firstSnapshot = snapshot(firstResult);

        resetSchema();
        ResourceSyncResult secondResult = handler.upsert(declaration.copy());
        RelationSnapshot secondSnapshot = snapshot(secondResult);

        long expectedId = PortableResourceIds.stable(
                TARGET_TABLE, TENANT_ID, MEMBER_ID, ORG_CODE);
        assertThat(secondSnapshot).isEqualTo(firstSnapshot);
        assertThat(secondResult.getTargetId()).isEqualTo(expectedId);
        assertThat(secondResult.getTargetTable()).isEqualTo(TARGET_TABLE);
    }

    @Test
    void upsert_declaredTargetId_usesDeclaredRelationId() {
        ResourceDeclaration declaration = resource();
        put(declaration, "targetId", ResourceFieldType.LONG, 7001L);

        ResourceSyncResult result = handler.upsert(declaration);

        assertThat(result.getTargetId()).isEqualTo(7001L);
        assertThat(jdbcTemplate.queryForObject(
                "select id from tenant_member_org", Long.class)).isEqualTo(7001L);
    }

    @Test
    void upsert_existingBusinessKey_preservesExistingRelationId() {
        jdbcTemplate.update("""
                insert into tenant_member_org
                    (id, tenant_id, member_id, org_id, primary_flag, leader_flag)
                values (?, ?, ?, ?, 0, 0)
                """, 9001L, TENANT_ID, MEMBER_ID, ORG_ID);
        ResourceDeclaration declaration = resource();
        put(declaration, "targetId", ResourceFieldType.LONG, 7001L);

        ResourceSyncResult result = handler.upsert(declaration);

        assertThat(result.getTargetId()).isEqualTo(9001L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_org", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForMap(
                "select post_id, primary_flag from tenant_member_org where id = 9001"))
                .containsEntry("post_id", POST_ID)
                .containsEntry("primary_flag", 1);
    }

    @Test
    void upsert_stableIdOccupiedByDifferentRelation_rejectsWithoutInsert() {
        long stableId = PortableResourceIds.stable(
                TARGET_TABLE, TENANT_ID, MEMBER_ID, ORG_CODE);
        jdbcTemplate.update("""
                insert into tenant_member_org
                    (id, tenant_id, member_id, org_id, primary_flag, leader_flag)
                values (?, ?, ?, ?, 0, 0)
                """, stableId, TENANT_ID, 9999L, 9999L);

        assertThatThrownBy(() -> handler.upsert(resource()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("portable relation ID collision")
                .hasMessageContaining(String.valueOf(stableId));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_org", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select primary_org_id from tenant_member where id = ?", Long.class, MEMBER_ID)).isNull();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists tenant_member_org");
        jdbcTemplate.execute("drop table if exists tenant_member");
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("""
                create table identity_user (
                    id bigint primary key,
                    username varchar(100) not null,
                    password varchar(255),
                    password_reset_required boolean not null default false,
                    password_updated_at timestamp,
                    nickname varchar(100),
                    realm varchar(32) not null default 'INTERNAL',
                    actor_type varchar(32) not null default 'INTERNAL_USER',
                    party_type varchar(32),
                    party_id bigint,
                    email varchar(128),
                    phone varchar(32),
                    avatar varchar(255),
                    real_name varchar(100),
                    document_type varchar(32),
                    document_number varchar(128),
                    verification_status varchar(32) not null default 'UNVERIFIED',
                    verification_source varchar(64),
                    status tinyint not null default 1,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    last_login_time timestamp,
                    failed_login_count int,
                    last_failed_login_at timestamp,
                    locked_until timestamp,
                    locked_reason varchar(100),
                    remark varchar(500),
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint primary key,
                    tenant_id bigint not null,
                    user_id bigint not null,
                    member_no varchar(64),
                    display_name varchar(100),
                    member_type varchar(32),
                    status tinyint not null default 1,
                    primary_org_id bigint,
                    primary_post_id bigint,
                    joined_at timestamp,
                    left_at timestamp,
                    remark varchar(500),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member_org (
                    id bigint primary key,
                    tenant_id bigint not null,
                    member_id bigint not null,
                    org_id bigint not null,
                    post_id bigint,
                    primary_flag tinyint not null default 0,
                    leader_flag tinyint not null default 0,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    unique (tenant_id, member_id, org_id)
                )
                """);
        jdbcTemplate.update("insert into identity_user (id, username, tenant_id) values (?, ?, ?)",
                USER_ID, "admin", String.valueOf(TENANT_ID));
        jdbcTemplate.update("""
                insert into tenant_member (id, tenant_id, user_id, member_no, status)
                values (?, ?, ?, ?, 1)
                """, MEMBER_ID, TENANT_ID, USER_ID, "ADMIN-default");
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.ORG_MEMBER_BINDING);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, TENANT_ID);
        put(resource, "username", ResourceFieldType.STRING, "admin");
        put(resource, "orgCode", ResourceFieldType.STRING, ORG_CODE);
        put(resource, "postCode", ResourceFieldType.STRING, POST_CODE);
        put(resource, "primaryOrg", ResourceFieldType.BOOLEAN, true);
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.putField(name, field);
    }

    private RelationSnapshot snapshot(ResourceSyncResult result) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, tenant_id, member_id, org_id, post_id, primary_flag, leader_flag
                from tenant_member_org
                order by id
                """);
        return new RelationSnapshot(result.getTargetId(), result.getTargetTable(), rows);
    }

    private record RelationSnapshot(Long targetId, String targetTable, List<Map<String, Object>> rows) {
    }

    @Configuration
    @MapperScan(basePackageClasses = IdentityUserMapper.class)
    @Import(OrgMemberBindingResourceHandler.class)
    static class TestConfig {

        @Bean
        OrgReferenceProvider orgReferenceProvider() {
            return new OrgReferenceProvider() {
                @Override
                public Long resolveOrgId(Long tenantId, String orgCode) {
                    return TENANT_ID == tenantId && ORG_CODE.equals(orgCode) ? ORG_ID : null;
                }

                @Override
                public Long resolvePostId(Long tenantId, String postCode) {
                    return TENANT_ID == tenantId && POST_CODE.equals(postCode) ? POST_ID : null;
                }
            };
        }
    }
}
