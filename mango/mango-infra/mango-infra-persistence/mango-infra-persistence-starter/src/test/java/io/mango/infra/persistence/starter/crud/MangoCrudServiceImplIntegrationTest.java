package io.mango.infra.persistence.starter.crud;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.common.po.PageQuery;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.crud.MangoCrudService;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.crud.QueryField;
import io.mango.infra.persistence.api.crud.QueryIgnore;
import io.mango.infra.persistence.api.crud.QueryType;
import io.mango.infra.persistence.api.entity.TenantEntity;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MangoCrudServiceImplIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mango_crud_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        }
)
class MangoCrudServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserCrudService userCrudService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS demo_user");
        jdbcTemplate.execute("""
                CREATE TABLE demo_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(64),
                    nickname VARCHAR(64),
                    status INT,
                    tenant_id VARCHAR(64),
                    created_by BIGINT,
                    created_at TIMESTAMP,
                    updated_by BIGINT,
                    updated_at TIMESTAMP,
                    org_id BIGINT
                )
                """);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "tenant-a", "tester", "default", "USER", "org", 3003L, "test"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void crudMethods_shouldPersistThroughMapperAndApplyTenantScope() {
        Long aliceId = (Long) userCrudService.createByCommand(new UserCreateCommand(
                10001L, "alice", "Alice", 1));
        Long bobId = (Long) userCrudService.createByCommand(new UserCreateCommand(
                10002L, "bob", "Bob", 1));
        insertOtherTenantUser();

        assertAuditFieldsFilledOnCreate(aliceId);

        assertThat(aliceId).isEqualTo(10001L);
        assertThat(userCrudService.updateByCommand(new UserUpdateCommand(aliceId, "Alice Updated", 2))).isTrue();
        assertAuditFieldsFilledOnUpdate(aliceId);

        UserVO detail = (UserVO) userCrudService.detailById(aliceId);
        assertThat(detail.nickname()).isEqualTo("Alice Updated");
        assertThat(detail.tenantId()).isEqualTo("tenant-a");

        UserQuery query = new UserQuery();
        query.setUsername("b");
        query.setStatuses(List.of(1, 2));
        List<?> list = userCrudService.listByQuery(query);
        assertThat(list)
                .extracting(row -> ((UserVO) row).username())
                .containsExactly("bob");

        query.setPage(1);
        query.setSize(1);
        PersistencePageResult<?> page = userCrudService.pageByQuery(query);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).hasSize(1);

        assertThat(userCrudService.batchDeleteByIds(List.of(aliceId, bobId))).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_user WHERE tenant_id = 'tenant-a'", Long.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_user WHERE tenant_id = 'tenant-b'", Long.class))
                .isEqualTo(1L);
    }

    private void insertOtherTenantUser() {
        jdbcTemplate.update("""
                INSERT INTO demo_user (id, username, nickname, status, tenant_id, created_by, created_at, updated_by, updated_at, org_id)
                VALUES (20001, 'mallory', 'Mallory', 1, 'tenant-b', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 9009)
                """);
    }

    private void assertAuditFieldsFilledOnCreate(Long id) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT tenant_id, created_by, created_at, updated_by, updated_at, org_id
                FROM demo_user
                WHERE id = ?
                """, id);
        assertThat(row.get("TENANT_ID")).isEqualTo("tenant-a");
        assertThat(row.get("CREATED_BY")).isEqualTo(1001L);
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_BY")).isEqualTo(1001L);
        assertThat(row.get("UPDATED_AT")).isNotNull();
        assertThat(row.get("ORG_ID")).isEqualTo(3003L);
    }

    private void assertAuditFieldsFilledOnUpdate(Long id) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT tenant_id, created_by, created_at, updated_by, updated_at, org_id
                FROM demo_user
                WHERE id = ?
                """, id);
        assertThat(row.get("TENANT_ID")).isEqualTo("tenant-a");
        assertThat(row.get("CREATED_BY")).isEqualTo(1001L);
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_BY")).isEqualTo(1001L);
        assertThat(row.get("UPDATED_AT")).isNotNull();
        assertThat(row.get("ORG_ID")).isEqualTo(3003L);
    }

    @SpringBootApplication
    @MapperScan(basePackageClasses = UserMapper.class)
    static class TestApplication {

        @Bean
        UserCrudService userCrudService() {
            return new UserCrudServiceImpl();
        }
    }

    interface UserMapper extends BaseMapper<UserEntity> {
    }

    interface UserCrudService extends MangoCrudService<UserEntity> {
    }

    @Service
    static class UserCrudServiceImpl extends MangoCrudServiceImpl<UserMapper, UserEntity>
            implements UserCrudService {

        @Override
        protected Class<UserEntity> entityType() {
            return UserEntity.class;
        }

        @Override
        protected Object toVO(UserEntity entity) {
            return new UserVO(entity.getId(), entity.getUsername(), entity.getNickname(), entity.getStatus(),
                    entity.getTenantId());
        }
    }

    @TableName("demo_user")
    static class UserEntity extends TenantEntity {

        private String username;

        private String nickname;

        private Integer status;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    record UserCreateCommand(Long id, String username, String nickname, Integer status) {
    }

    record UserUpdateCommand(Long id, String nickname, Integer status) {
    }

    static class UserQuery extends PageQuery {

        @QueryField(type = QueryType.RIGHT_LIKE)
        private String username;

        @QueryField(column = "status", type = QueryType.IN)
        private List<Integer> statuses;

        @QueryIgnore
        private String ignored;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public List<Integer> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<Integer> statuses) {
            this.statuses = statuses;
        }

        public String getIgnored() {
            return ignored;
        }

        public void setIgnored(String ignored) {
            this.ignored = ignored;
        }
    }

    record UserVO(Long id, String username, String nickname, Integer status, String tenantId) {
    }
}
