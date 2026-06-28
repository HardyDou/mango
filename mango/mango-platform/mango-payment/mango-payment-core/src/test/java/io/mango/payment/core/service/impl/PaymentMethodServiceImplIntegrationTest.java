package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.mapper.PaymentMethodCategoryMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PaymentMethodServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_method_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/payment/*.xml",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
public class PaymentMethodServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentMethodMapper methodMapper;

    @Autowired
    private PaymentMethodServiceImpl service;

    @Autowired
    private TestPaymentOperationAuditService auditService;

    @BeforeEach
    void setUp() {
        resetSchema();
        auditService.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createMethodValidatesCategoryPathAndPersistsNormalizedMethodThroughRealMappers() {
        insertWechatCategoryPath();

        Long id = service.createMethod(command()).getData();

        PaymentMethod entity = methodMapper.selectById(id);
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getChannelId()).isNull();
        assertThat(entity.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR_TEST");
        assertThat(entity.getMethodName()).isEqualTo("微信扫码测试");
        assertThat(entity.getAccountNature()).isEqualTo("PERSONAL");
        assertThat(entity.getInstrumentType()).isEqualTo("WECHAT");
        assertThat(entity.getInteractionType()).isEqualTo("QR_CODE");
        assertThat(entity.getCashierGroupCode()).isEqualTo("WECHAT_PAY");
        assertThat(entity.getCashierGroupName()).isEqualTo("微信支付");
        assertThat(entity.getCashierGroupSort()).isEqualTo(10);
        assertThat(entity.getRequiresQrRefresh()).isEqualTo(1);
        assertThat(auditService.records).containsExactly(
                "CREATE_METHOD|PAYMENT_METHOD|PERSONAL_WECHAT_QR_TEST|SUCCESS");
    }

    @Test
    void createMethodRejectsDuplicateMethodCodeThroughRealSelectCount() {
        insertWechatCategoryPath();
        insertMethod(340001L, "PERSONAL_WECHAT_QR_TEST");

        assertThatThrownBy(() -> service.createMethod(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式编码不能重复");

        assertThat(activeMethodCount("PERSONAL_WECHAT_QR_TEST")).isEqualTo(1L);
    }

    @Test
    void createMethodRejectsInvalidCategoryPathBeforeInsert() {
        insertCategory(360001L, "PERSONAL", "对私", 1, 0L, 10);
        insertCategory(360101L, "WECHAT", "微信", 2, 360001L, 10);

        assertThatThrownBy(() -> service.createMethod(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式三级分类不正确");

        assertThat(methodMapper.selectCount(null)).isZero();
    }

    @Test
    void createMethodAcceptsBankCardDebitQuickStandardHierarchyThroughRealCategoryMapper() {
        insertBankCardDebitQuickCategoryPath();
        SavePaymentMethodCommand command = command();
        command.setMethodCode("PERSONAL_DEBIT_QUICK_TEST");
        command.setMethodName("储蓄卡快捷测试");
        command.setInstrumentType("BANK_CARD");
        command.setInteractionType("DEBIT_QUICK");
        command.setPaymentMaterialType("HTML_FORM");
        command.setRequiresBankSelection(1);

        Long id = service.createMethod(command).getData();

        PaymentMethod entity = methodMapper.selectById(id);
        assertThat(entity.getAccountNature()).isEqualTo("PERSONAL");
        assertThat(entity.getInstrumentType()).isEqualTo("BANK_CARD");
        assertThat(entity.getInteractionType()).isEqualTo("DEBIT_QUICK");
        assertThat(entity.getPaymentMaterialType()).isEqualTo("HTML_FORM");
        assertThat(entity.getRequiresBankSelection()).isEqualTo(1);
    }

    @Test
    void deleteMethodRejectsWhenCashierConfigReferencesMethodThroughRealMapperSql() {
        insertWechatCategoryPath();
        insertMethod(340001L, "PERSONAL_WECHAT_QR");
        insertCashierConfig(350001L, "PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT", "PERSONAL_WECHAT_QR", "", 0);

        assertThatThrownBy(() -> service.deleteMethod(340001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_METHOD_DELETE_HAS_RELATIONS.getMessage());

        assertThat(methodMapper.selectById(340001L)).isNotNull();
        assertThat(auditService.records).containsExactly(
                "DELETE_METHOD|PAYMENT_METHOD|PERSONAL_WECHAT_QR|REJECTED");
    }

    @Test
    void deleteMethodLogicalDeletesWhenNoRelationsThroughRealMapperSql() {
        insertWechatCategoryPath();
        insertMethod(340001L, "PERSONAL_WECHAT_QR");
        insertCashierConfig(350001L, "PERSONAL_WECHAT_QR", "PERSONAL_WECHAT_QR", "PERSONAL_WECHAT_QR", 1);

        service.deleteMethod(340001L);

        assertThat(methodMapper.selectById(340001L)).isNull();
        assertThat(countDeletedMethods()).isEqualTo(1L);
        assertThat(auditService.records).containsExactly(
                "DELETE_METHOD|PAYMENT_METHOD|PERSONAL_WECHAT_QR|SUCCESS");
    }

    @Test
    void listMethodCategoriesReturnsOrderedTreeFromRealMapper() {
        insertWechatCategoryPath();

        List<PaymentMethodCategoryVO> tree = service.listMethodCategories().getData();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getCategoryCode()).isEqualTo("PERSONAL");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getCategoryCode()).isEqualTo("WECHAT");
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getCategoryCode()).isEqualTo("QR_CODE");
    }

    private SavePaymentMethodCommand command() {
        SavePaymentMethodCommand command = new SavePaymentMethodCommand();
        command.setMethodCode("  PERSONAL_WECHAT_QR_TEST  ");
        command.setMethodName("  微信扫码测试  ");
        command.setAccountNature("PERSONAL");
        command.setInstrumentType("WECHAT");
        command.setInteractionType("QR_CODE");
        command.setTerminalScope("WEB,H5");
        command.setPaymentMaterialType("QR");
        command.setCashierGroupCode("WECHAT_PAY");
        command.setCashierGroupName("微信支付");
        command.setCashierGroupSort(10);
        command.setRequiresQrRefresh(1);
        command.setSort(100);
        command.setStatus(1);
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("create alias if not exists find_in_set for \""
                + PaymentMethodServiceImplIntegrationTest.class.getName() + ".findInSet\"");
        jdbcTemplate.execute("drop table if exists payment_virtual_channel_payment");
        jdbcTemplate.execute("drop table if exists payment_order");
        jdbcTemplate.execute("drop table if exists payment_cashier_config");
        jdbcTemplate.execute("drop table if exists payment_method_route_rule");
        jdbcTemplate.execute("drop table if exists payment_channel_contract_capability");
        jdbcTemplate.execute("drop table if exists payment_channel_capability");
        jdbcTemplate.execute("drop table if exists payment_method");
        jdbcTemplate.execute("drop table if exists payment_method_category");
        createMethodCategoryTable();
        createMethodTable();
        createRelationTables();
    }

    private void createMethodCategoryTable() {
        jdbcTemplate.execute("""
                create table payment_method_category (
                    id bigint primary key,
                    category_code varchar(128),
                    category_name varchar(128),
                    level int,
                    parent_id bigint,
                    sort int,
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createMethodTable() {
        jdbcTemplate.execute("""
                create table payment_method (
                    id bigint primary key,
                    method_code varchar(128),
                    method_name varchar(128),
                    channel_id bigint,
                    account_nature varchar(64),
                    instrument_type varchar(64),
                    interaction_type varchar(64),
                    terminal_scope varchar(64),
                    payment_material_type varchar(64),
                    cashier_group_code varchar(64),
                    cashier_group_name varchar(128),
                    cashier_group_sort int,
                    icon_file_id bigint,
                    requires_bank_selection int,
                    requires_qr_refresh int,
                    description varchar(512),
                    sort int,
                    status int,
                    tenant_id bigint,
                    del_flag int default 0,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createRelationTables() {
        jdbcTemplate.execute("""
                create table payment_channel_capability (
                    id bigint primary key,
                    method_code varchar(128),
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_channel_contract_capability (
                    id bigint primary key,
                    method_code varchar(128),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_method_route_rule (
                    id bigint primary key,
                    method_code varchar(128),
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_cashier_config (
                    id bigint primary key,
                    method_codes varchar(512),
                    default_method_code varchar(128),
                    method_display_order varchar(512),
                    tenant_id bigint,
                    del_flag int default 0
                )
                """);
        jdbcTemplate.execute("""
                create table payment_order (
                    id bigint primary key,
                    method_id bigint,
                    tenant_id bigint
                )
                """);
        jdbcTemplate.execute("""
                create table payment_virtual_channel_payment (
                    id bigint primary key,
                    payment_method_id bigint,
                    payment_method_code varchar(128),
                    tenant_id bigint
                )
                """);
    }

    private void insertWechatCategoryPath() {
        insertCategory(360001L, "PERSONAL", "对私", 1, 0L, 10);
        insertCategory(360101L, "WECHAT", "微信", 2, 360001L, 10);
        insertCategory(360301L, "QR_CODE", "扫码", 3, 360101L, 10);
    }

    private void insertBankCardDebitQuickCategoryPath() {
        insertCategory(360001L, "PERSONAL", "对私", 1, 0L, 10);
        insertCategory(360104L, "BANK_CARD", "银行卡", 2, 360001L, 40);
        insertCategory(360331L, "DEBIT_QUICK", "储蓄卡快捷", 3, 360104L, 10);
    }

    private void insertCategory(Long id, String code, String name, Integer level, Long parentId, Integer sort) {
        jdbcTemplate.update("""
                        insert into payment_method_category (
                            id, category_code, category_name, level, parent_id, sort, status, tenant_id,
                            del_flag, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, 1, 1, 0, current_timestamp, current_timestamp)
                        """,
                id, code, name, level, parentId, sort);
    }

    private void insertMethod(Long id, String methodCode) {
        jdbcTemplate.update("""
                        insert into payment_method (
                            id, method_code, method_name, account_nature, instrument_type, interaction_type,
                            terminal_scope, payment_material_type, cashier_group_code, cashier_group_name,
                            cashier_group_sort, requires_bank_selection, requires_qr_refresh, sort, status,
                            tenant_id, del_flag, created_at, updated_at
                        ) values (?, ?, '微信扫码', 'PERSONAL', 'WECHAT', 'QR_CODE',
                            'WEB,H5', 'QR', 'WECHAT_PAY', '微信支付',
                            10, 0, 1, 100, 1, 1, 0, current_timestamp, current_timestamp)
                        """,
                id, methodCode);
    }

    private void insertCashierConfig(Long id, String methodCodes, String defaultMethodCode,
                                     String displayOrder, Integer delFlag) {
        jdbcTemplate.update("""
                        insert into payment_cashier_config (
                            id, method_codes, default_method_code, method_display_order, tenant_id, del_flag
                        ) values (?, ?, ?, ?, 1, ?)
                        """,
                id, methodCodes, defaultMethodCode, displayOrder, delFlag);
    }

    private Long activeMethodCount(String methodCode) {
        return methodMapper.selectCount(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getMethodCode, methodCode));
    }

    private Long countDeletedMethods() {
        return jdbcTemplate.queryForObject(
                "select count(1) from payment_method where id = 340001 and del_flag = 1",
                Long.class);
    }

    public static boolean findInSet(String value, String csv) {
        if (value == null || csv == null || csv.isBlank()) {
            return false;
        }
        for (String item : csv.split(",")) {
            if (value.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    @Configuration
    @MapperScan(basePackageClasses = PaymentMethodMapper.class)
    @Import(PaymentMethodServiceImpl.class)
    static class TestConfig {

        @Bean
        TestPaymentOperationAuditService paymentOperationAuditService() {
            return new TestPaymentOperationAuditService();
        }
    }

    static class TestPaymentOperationAuditService extends PaymentOperationAuditService {

        private final List<String> records = new ArrayList<>();

        TestPaymentOperationAuditService() {
            super(null);
        }

        @Override
        public void record(String operationAction, String resourceType, String resourceId, String operationResult) {
            records.add(operationAction + "|" + resourceType + "|" + resourceId + "|" + operationResult);
        }

        void clear() {
            records.clear();
        }
    }
}
