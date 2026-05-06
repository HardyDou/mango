package io.mango.infra.persistence.starter.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.po.PageQuery;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;
import io.mango.infra.persistence.api.query.PersistencePageResult;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MultiTableQueryServiceIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mango_multi_table_query;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.schema-validation.enabled=false",
                "mybatis-plus.mapper-locations=classpath:/mapper/query/*.xml"
        }
)
class MultiTableQueryServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserDeptQueryService queryService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS demo_user");
        jdbcTemplate.execute("DROP TABLE IF EXISTS demo_dept");
        jdbcTemplate.execute("""
                CREATE TABLE demo_dept (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(64),
                    tenant_id VARCHAR(64),
                    created_by BIGINT,
                    created_at TIMESTAMP,
                    updated_by BIGINT,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE demo_user (
                    id BIGINT PRIMARY KEY,
                    dept_id BIGINT,
                    username VARCHAR(64),
                    status INT,
                    tenant_id VARCHAR(64),
                    created_by BIGINT,
                    created_at TIMESTAMP,
                    updated_by BIGINT,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO demo_dept (id, name, tenant_id, created_by, created_at, updated_by, updated_at)
                VALUES
                  (10, 'Engineering', 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (11, 'Finance', 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (20, 'Engineering', 'tenant-b', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO demo_user (id, dept_id, username, status, tenant_id, created_by, created_at, updated_by, updated_at)
                VALUES
                  (10001, 10, 'alice', 1, 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (10002, 10, 'alex', 1, 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (10003, 11, 'bob', 1, 'tenant-a', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
                  (20001, 20, 'alice', 1, 'tenant-b', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
                """);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "tenant-a", "tester", "default", "USER", "USER", 1001L, "test"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void pageUserWithDept_shouldJoinTwoTablesAndKeepTenantScope() {
        UserDeptQuery query = new UserDeptQuery();
        query.setPage(1);
        query.setSize(1);
        query.setUsernamePrefix("al");
        query.setDeptName("Engineering");

        PersistencePageResult<UserDeptVO> page = queryService.pageUserWithDept(query);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getPages()).isEqualTo(2);
        assertThat(page.getRecords())
                .extracting(UserDeptVO::getUsername)
                .containsExactly("alex");

        query.setPage(2);
        PersistencePageResult<UserDeptVO> secondPage = queryService.pageUserWithDept(query);

        assertThat(secondPage.getRecords())
                .extracting(UserDeptVO::getUsername)
                .containsExactly("alice");
        assertThat(secondPage.getRecords())
                .extracting(UserDeptVO::getTenantId)
                .containsOnly("tenant-a");
        assertThat(secondPage.getRecords())
                .extracting(UserDeptVO::getDeptName)
                .containsOnly("Engineering");
    }

    @SpringBootApplication
    @MapperScan(basePackageClasses = UserDeptQueryMapper.class)
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        UserDeptQueryService userDeptQueryService(UserDeptQueryMapper mapper,
                                                  PersistenceContextProvider contextProvider) {
            return new UserDeptQueryService(mapper, contextProvider);
        }
    }

    static class UserDeptQueryService extends MangoQueryServiceSupport {

        private final UserDeptQueryMapper mapper;

        UserDeptQueryService(UserDeptQueryMapper mapper, PersistenceContextProvider contextProvider) {
            super(contextProvider);
            this.mapper = mapper;
        }

        PersistencePageResult<UserDeptVO> pageUserWithDept(UserDeptQuery query) {
            IPage<UserDeptVO> result = mapper.pageUserWithDept(page(query), query, tenantId());
            return pageResult(result);
        }
    }

    @Mapper
    interface UserDeptQueryMapper {

        IPage<UserDeptVO> pageUserWithDept(Page<UserDeptVO> page,
                                           @Param("query") UserDeptQuery query,
                                           @Param("tenantId") String tenantId);
    }

    static class UserDeptQuery extends PageQuery {

        private String usernamePrefix;

        private String deptName;

        public String getUsernamePrefix() {
            return usernamePrefix;
        }

        public void setUsernamePrefix(String usernamePrefix) {
            this.usernamePrefix = usernamePrefix;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }
    }

    static class UserDeptVO {

        private Long userId;

        private String username;

        private Integer status;

        private String tenantId;

        private Long deptId;

        private String deptName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }
    }
}
