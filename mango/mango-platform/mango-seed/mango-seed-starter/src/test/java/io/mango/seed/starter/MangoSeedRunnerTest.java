package io.mango.seed.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MangoSeedRunnerTest {

    private JdbcTemplate jdbcTemplate;
    private MangoSeedProperties properties;
    private ConfigurableEnvironment environment;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        createTables();
        seedOfficialPackage();
        properties = new MangoSeedProperties();
        properties.getAdmin().setInitialPassword("ChangeMe-12345");
        environment = new StandardEnvironment();
    }

    @Test
    void seedCreatesOfficialEntryDataAndIsIdempotent() {
        runner().seed();
        runner().seed();

        Long tenantId = jdbcTemplate.queryForObject("select id from sys_tenant where tenant_code = 'default'", Long.class);
        Long userId = jdbcTemplate.queryForObject("select id from identity_user where realm = 'INTERNAL' and username = 'admin'", Long.class);
        Long memberId = jdbcTemplate.queryForObject("select id from tenant_member where tenant_id = ? and user_id = ?", Long.class, tenantId, userId);
        Long roleId = jdbcTemplate.queryForObject("""
                select id from authorization_role where tenant_id = ? and app_code = 'internal-admin' and role_code = 'ROLE_ADMIN'
                """, Long.class, tenantId);

        assertThat(count("sys_tenant")).isEqualTo(1);
        assertThat(count("identity_user")).isEqualTo(1);
        assertThat(count("tenant_member")).isEqualTo(1);
        assertThat(count("authorization_role")).isEqualTo(1);
        assertThat(count("authorization_subject_role")).isEqualTo(1);
        assertThat(count("frontend_tenant_app_binding")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from authorization_role_menu where role_id = ?", Integer.class, roleId))
                .isEqualTo(3);
        assertThat(memberId).isNotNull();
    }

    @Test
    void seedDoesNotResetExistingAdminPassword() {
        runner().seed();
        String password = jdbcTemplate.queryForObject("select password from identity_user where username = 'admin'", String.class);

        properties.getAdmin().setInitialPassword("Another-Password-12345");
        runner().seed();

        assertThat(jdbcTemplate.queryForObject("select password from identity_user where username = 'admin'", String.class))
                .isEqualTo(password);
    }

    @Test
    void newAdminRequiresInitialPassword() {
        properties.getAdmin().setInitialPassword("");

        assertThatThrownBy(() -> runner().seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mango.seed.admin.initial-password");
    }

    @Test
    void productionProfileRejectsKnownWeakPassword() {
        properties.getAdmin().setInitialPassword("admin123");
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> runner().seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-default");
    }

    private MangoSeedRunner runner() {
        return new MangoSeedRunner(jdbcTemplate, properties, new BCryptPasswordEncoder(), environment);
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
    }

    private void seedOfficialPackage() {
        jdbcTemplate.update("""
                insert into authorization_menu_package
                (id, tenant_id, package_name, package_code, app_code, status, sort, remark, create_time, update_time, del_flag)
                values (1, 1, '平台管理套餐', 'platform_admin', 'internal-admin', 1, 1, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """);
        jdbcTemplate.update("""
                insert into authorization_menu_package_item (id, tenant_id, package_id, menu_id, sort)
                values (1, 1, 1, 10, 1), (2, 1, 1, 20, 2), (3, 1, 1, 30, 3)
                """);
    }

    private void createTables() {
        jdbcTemplate.execute("""
                create table sys_tenant (
                  id bigint primary key,
                  tenant_name varchar(100) not null,
                  tenant_code varchar(50) not null unique,
                  status tinyint not null,
                  contact varchar(64),
                  mobile varchar(20),
                  email varchar(100),
                  remark varchar(500),
                  create_by varchar(64),
                  update_by varchar(64),
                  create_time timestamp,
                  update_time timestamp,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  tenant_id varchar(64) not null,
                  institution_type varchar(32) not null,
                  capability_codes varchar(500)
                )
                """);
        jdbcTemplate.execute("""
                create table identity_user (
                  id bigint primary key,
                  username varchar(100) not null,
                  password varchar(200) not null,
                  nickname varchar(100),
                  realm varchar(32) not null,
                  actor_type varchar(32) not null,
                  party_type varchar(64),
                  party_id bigint,
                  email varchar(100),
                  phone varchar(32),
                  avatar varchar(500),
                  status tinyint not null,
                  create_time timestamp,
                  update_time timestamp,
                  last_login_time timestamp,
                  remark varchar(500),
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  tenant_id varchar(64) not null,
                  unique (realm, username)
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member (
                  id bigint primary key,
                  tenant_id bigint not null,
                  user_id bigint not null,
                  member_no varchar(64),
                  display_name varchar(100) not null,
                  member_type varchar(32) not null,
                  status tinyint not null,
                  primary_org_id bigint,
                  primary_post_id bigint,
                  joined_at timestamp,
                  left_at timestamp,
                  remark varchar(500),
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  unique (tenant_id, user_id)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_role (
                  id bigint primary key,
                  tenant_id bigint not null,
                  app_code varchar(64) not null,
                  realm varchar(32) not null,
                  actor_type varchar(32),
                  role_code varchar(100) not null,
                  role_name varchar(50) not null,
                  role_type tinyint not null,
                  status tinyint not null,
                  sort int not null,
                  create_time timestamp,
                  update_time timestamp,
                  remark varchar(500),
                  del_flag tinyint not null,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp,
                  unique (tenant_id, app_code, role_code)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_subject_role (
                  id bigint primary key,
                  tenant_id bigint not null,
                  subject_id bigint not null,
                  subject_type varchar(32) not null,
                  app_code varchar(64),
                  realm varchar(32),
                  actor_type varchar(32),
                  party_type varchar(64),
                  party_id bigint,
                  role_id bigint not null,
                  create_time timestamp,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table frontend_tenant_app_binding (
                  id bigint primary key,
                  tenant_id bigint not null,
                  app_code varchar(64) not null,
                  status tinyint not null,
                  expire_time timestamp,
                  create_time timestamp,
                  update_time timestamp,
                  unique (tenant_id, app_code)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu_package (
                  id bigint primary key,
                  tenant_id bigint not null,
                  package_name varchar(100) not null,
                  package_code varchar(64) not null,
                  app_code varchar(64) not null,
                  status tinyint not null,
                  sort int not null,
                  remark varchar(500),
                  create_time timestamp,
                  update_time timestamp,
                  del_flag tinyint not null
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu_package_item (
                  id bigint primary key,
                  tenant_id bigint not null,
                  package_id bigint not null,
                  menu_id bigint not null,
                  sort int not null
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_role_menu (
                  id bigint primary key,
                  tenant_id bigint not null,
                  role_id bigint not null,
                  menu_id bigint not null,
                  create_time timestamp,
                  created_by bigint,
                  created_at timestamp,
                  updated_by bigint,
                  updated_at timestamp
                )
                """);
    }
}
