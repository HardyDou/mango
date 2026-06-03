package io.mango.seed.starter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

public class MangoSeedRunner implements ApplicationRunner {

    static final String APP_CODE = "internal-admin";
    static final String REALM = "INTERNAL";
    static final String ACTOR_TYPE = "INTERNAL_USER";
    static final String PARTY_TYPE = "INTERNAL_ORG";
    static final String ROLE_CODE = "ROLE_ADMIN";

    private final JdbcTemplate jdbcTemplate;
    private final MangoSeedProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public MangoSeedRunner(JdbcTemplate jdbcTemplate,
                           MangoSeedProperties properties,
                           PasswordEncoder passwordEncoder) {
        this(jdbcTemplate, properties, passwordEncoder, null);
    }

    public MangoSeedRunner(JdbcTemplate jdbcTemplate,
                           MangoSeedProperties properties,
                           PasswordEncoder passwordEncoder,
                           Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    void seed() {
        long tenantId = ensureTenant();
        long userId = ensureAdminUser();
        long memberId = ensureTenantMember(tenantId, userId);
        long roleId = ensureAdminRole(tenantId);
        ensureSubjectRole(tenantId, memberId, roleId);
        ensureTenantAppBinding(tenantId);
        syncRoleMenusFromPackage(tenantId, roleId);
    }

    private long ensureTenant() {
        MangoSeedProperties.Tenant tenant = properties.getTenant();
        Optional<Long> existing = queryLong("select id from sys_tenant where tenant_code = ? order by id limit 1", tenant.getCode());
        if (existing.isPresent()) {
            return existing.get();
        }
        long tenantId = IdWorker.getId();
        jdbcTemplate.update("""
                insert into sys_tenant
                (id, tenant_name, tenant_code, status, contact, mobile, email, remark,
                 create_by, update_by, create_time, update_time, created_by, created_at, updated_by, updated_at,
                 tenant_id, institution_type, capability_codes)
                values (?, ?, ?, 1, ?, ?, ?, ?, null, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                        null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP, 'default', ?, ?)
                """,
                tenantId,
                tenant.getName(),
                tenant.getCode(),
                properties.getAdmin().getNickname(),
                blankToNull(properties.getAdmin().getPhone()),
                blankToNull(properties.getAdmin().getEmail()),
                "Mango official seed tenant",
                tenant.getInstitutionType(),
                capabilityCodes(tenant.getCapabilityCodes()));
        return tenantId;
    }

    private long ensureAdminUser() {
        MangoSeedProperties.Admin admin = properties.getAdmin();
        Optional<Long> existing = queryLong("""
                select id from identity_user where realm = ? and username = ? order by id limit 1
                """, REALM, admin.getUsername());
        if (existing.isPresent()) {
            return existing.get();
        }
        requireInitialPasswordForNewAdmin();
        long userId = IdWorker.getId();
        jdbcTemplate.update("""
                insert into identity_user
                (id, username, password, nickname, realm, actor_type, party_type, party_id,
                 email, phone, avatar, status, create_time, update_time, last_login_time, remark,
                 created_by, created_at, updated_by, updated_at, tenant_id)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                        null, 'Mango official seed admin', null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP, 'default')
                """,
                userId,
                admin.getUsername(),
                passwordEncoder.encode(admin.getInitialPassword()),
                admin.getNickname(),
                REALM,
                ACTOR_TYPE,
                PARTY_TYPE,
                1L,
                blankToNull(admin.getEmail()),
                blankToNull(admin.getPhone()));
        return userId;
    }

    private long ensureTenantMember(long tenantId, long userId) {
        Optional<Long> existing = queryLong("""
                select id from tenant_member where tenant_id = ? and user_id = ? order by id limit 1
                """, tenantId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        long memberId = IdWorker.getId();
        jdbcTemplate.update("""
                insert into tenant_member
                (id, tenant_id, user_id, member_no, display_name, member_type, status,
                 primary_org_id, primary_post_id, joined_at, left_at, remark,
                 created_by, created_at, updated_by, updated_at)
                values (?, ?, ?, ?, ?, 'INSTITUTION_ADMIN', 1, null, null, CURRENT_TIMESTAMP, null,
                        'Mango official seed tenant admin member', null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP)
                """,
                memberId,
                tenantId,
                userId,
                "ADMIN-" + properties.getTenant().getCode(),
                properties.getAdmin().getNickname());
        return memberId;
    }

    private long ensureAdminRole(long tenantId) {
        Optional<Long> existing = queryLong("""
                select id from authorization_role
                where tenant_id = ? and app_code = ? and role_code = ? order by id limit 1
                """, tenantId, appCode(), ROLE_CODE);
        if (existing.isPresent()) {
            return existing.get();
        }
        long roleId = IdWorker.getId();
        jdbcTemplate.update("""
                insert into authorization_role
                (id, tenant_id, app_code, realm, actor_type, role_code, role_name, role_type, status, sort,
                 create_time, update_time, remark, del_flag, created_by, created_at, updated_by, updated_at)
                values (?, ?, ?, ?, ?, ?, '超级管理员', 1, 1, 1,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Mango official seed admin role', 0,
                        null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP)
                """, roleId, tenantId, appCode(), REALM, ACTOR_TYPE, ROLE_CODE);
        return roleId;
    }

    private void ensureSubjectRole(long tenantId, long memberId, long roleId) {
        int exists = jdbcTemplate.queryForObject("""
                select count(*) from authorization_subject_role
                where subject_type = 'TENANT_MEMBER' and subject_id = ? and role_id = ?
                  and tenant_id = ? and app_code = ? and party_type = ? and party_id = ?
                """, Integer.class, memberId, roleId, tenantId, appCode(), PARTY_TYPE, tenantId);
        if (exists > 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into authorization_subject_role
                (id, tenant_id, subject_id, subject_type, app_code, realm, actor_type, party_type, party_id, role_id,
                 create_time, created_by, created_at, updated_by, updated_at)
                values (?, ?, ?, 'TENANT_MEMBER', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                        null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP)
                """, IdWorker.getId(), tenantId, memberId, appCode(), REALM, ACTOR_TYPE, PARTY_TYPE, tenantId, roleId);
    }

    private void ensureTenantAppBinding(long tenantId) {
        int exists = jdbcTemplate.queryForObject("""
                select count(*) from frontend_tenant_app_binding where tenant_id = ? and app_code = ?
                """, Integer.class, tenantId, appCode());
        if (exists > 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into frontend_tenant_app_binding
                (id, tenant_id, app_code, status, create_time, update_time)
                values (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, IdWorker.getId(), tenantId, appCode());
    }

    private void syncRoleMenusFromPackage(long tenantId, long roleId) {
        Optional<Long> packageId = queryLong("""
                select id from authorization_menu_package
                where tenant_id = 1 and package_code = ? and app_code = ? and status = 1 and del_flag = 0
                order by id limit 1
                """, properties.getTenant().getPackageCode(), appCode());
        if (packageId.isEmpty()) {
            return;
        }
        List<Long> menuIds = jdbcTemplate.queryForList("""
                select menu_id from authorization_menu_package_item
                where tenant_id = 1 and package_id = ?
                """, Long.class, packageId.get());
        for (Long menuId : menuIds) {
            int exists = jdbcTemplate.queryForObject("""
                    select count(*) from authorization_role_menu
                    where tenant_id = ? and role_id = ? and menu_id = ?
                    """, Integer.class, tenantId, roleId, menuId);
            if (exists == 0) {
                jdbcTemplate.update("""
                        insert into authorization_role_menu
                        (id, tenant_id, role_id, menu_id, create_time, created_by, created_at, updated_by, updated_at)
                        values (?, ?, ?, ?, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP)
                        """, IdWorker.getId(), tenantId, roleId, menuId);
            }
        }
    }

    private void requireInitialPasswordForNewAdmin() {
        String initialPassword = properties.getAdmin().getInitialPassword();
        if (StringUtils.hasText(initialPassword) && !isProductionProfile()) {
            return;
        }
        if (StringUtils.hasText(initialPassword) && !isKnownWeakPassword(initialPassword)) {
            return;
        }
        throw new IllegalStateException("""
                mango.seed.admin.initial-password must be explicitly configured with a non-default value before creating the initial admin account.
                """.trim());
    }

    private boolean isProductionProfile() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            String normalized = profile.toLowerCase(Locale.ROOT);
            if ("prod".equals(normalized) || "production".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnownWeakPassword(String password) {
        return Set.of("admin", "admin123", "123456", "password").contains(password.toLowerCase(Locale.ROOT));
    }

    private Optional<Long> queryLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return values.stream().findFirst();
    }

    private String appCode() {
        String appCode = properties.getTenant().getAppCode();
        return StringUtils.hasText(appCode) ? appCode : APP_CODE;
    }

    private String capabilityCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
