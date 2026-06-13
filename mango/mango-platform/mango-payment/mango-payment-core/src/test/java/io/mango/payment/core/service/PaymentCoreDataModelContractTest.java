package io.mango.payment.core.service;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelBillBatchEntity;
import io.mango.payment.core.entity.PaymentChannelBillDetailEntity;
import io.mango.payment.core.entity.PaymentChannelCapability;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.entity.PaymentChannelFieldTemplateEntity;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentMethodCategory;
import io.mango.payment.core.entity.PaymentMethodRouteRule;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItem;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.entity.PaymentOfflineBankStatementBatchEntity;
import io.mango.payment.core.entity.PaymentOfflineBankStatementItemEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionMatchEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionVoucherEntity;
import io.mango.payment.core.entity.PaymentOfflineRefundEntity;
import io.mango.payment.core.entity.PaymentOperationAudit;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentRiskRuleEntity;
import io.mango.payment.core.entity.PaymentSettlementSummaryEntity;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.entity.PaymentTenantEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelBillBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillDetailMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
import io.mango.payment.core.mapper.PaymentChannelFieldTemplateMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodCategoryMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleItemMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementBatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementItemMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionVoucherMapper;
import io.mango.payment.core.mapper.PaymentOfflineRefundMapper;
import io.mango.payment.core.mapper.PaymentOperationAuditMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentRiskRuleMapper;
import io.mango.payment.core.mapper.PaymentSettlementSummaryMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
import io.mango.payment.core.mapper.PaymentTenantMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCoreDataModelContractTest {

    private static final Pattern DESIGN_TABLE_PATTERN = Pattern.compile("^\\| (pay_[a-z_]+) \\|", Pattern.MULTILINE);
    private static final String DESIGN_DIR = "../../../../mango-docs/designs";
    private static final String MIGRATION_DIR = "src/main/resources/db/migration/payment";

    private static final Map<String, ModelContract> CONTRACTS = new LinkedHashMap<>();

    static {
        contract("pay_tenant", "payment_tenant", PaymentTenantEntity.class, PaymentTenantMapper.class);
        contract("pay_app", "payment_application", PaymentApplication.class, PaymentApplicationMapper.class);
        contract("pay_subject", "payment_enterprise_subject", PaymentEnterpriseSubject.class, PaymentEnterpriseSubjectMapper.class);
        contract("pay_subject_bank_account", "payment_subject_bank_account",
                PaymentSubjectBankAccountEntity.class, PaymentSubjectBankAccountMapper.class);
        contract("pay_channel", "payment_channel", PaymentChannel.class, PaymentChannelMapper.class);
        contract("pay_channel_capability", "payment_channel_capability",
                PaymentChannelCapability.class, PaymentChannelCapabilityMapper.class);
        contract("pay_channel_field_template", "payment_channel_field_template",
                PaymentChannelFieldTemplateEntity.class, PaymentChannelFieldTemplateMapper.class);
        contract("pay_channel_contract", "payment_channel_contract",
                PaymentChannelContract.class, PaymentChannelContractMapper.class);
        contract("pay_channel_contract_value", "payment_channel_contract_value",
                PaymentChannelContractValueEntity.class, PaymentChannelContractValueMapper.class);
        contract("pay_channel_contract_capability", "payment_channel_contract_capability",
                PaymentChannelContractCapability.class, PaymentChannelContractCapabilityMapper.class);
        contract("pay_method_category", "payment_method_category", PaymentMethodCategory.class, PaymentMethodCategoryMapper.class);
        contract("pay_method", "payment_method", PaymentMethod.class, PaymentMethodMapper.class);
        contract("pay_method_route_rule", "payment_method_route_rule",
                PaymentMethodRouteRule.class, PaymentMethodRouteRuleMapper.class);
        contract("pay_method_route_rule_item", "payment_method_route_rule_item",
                PaymentMethodRouteRuleItem.class, PaymentMethodRouteRuleItemMapper.class);
        contract("pay_cashier", "payment_cashier_config", PaymentCashierConfig.class, PaymentCashierConfigMapper.class);
        contract("pay_biz_order", "payment_business_order", PaymentBusinessOrderEntity.class, PaymentBusinessOrderMapper.class);
        contract("pay_payment_order", "payment_order", PaymentOrderEntity.class, PaymentOrderMapper.class);
        contract("pay_refund_order", "payment_refund_order", PaymentRefundOrderEntity.class, PaymentRefundOrderMapper.class);
        contract("pay_transaction_flow", "payment_transaction_flow",
                PaymentTransactionFlowEntity.class, PaymentTransactionFlowMapper.class);
        contract("pay_offline_collection", "payment_offline_collection",
                PaymentOfflineCollectionEntity.class, PaymentOfflineCollectionMapper.class);
        contract("pay_offline_collection_voucher", "payment_offline_collection_voucher",
                PaymentOfflineCollectionVoucherEntity.class, PaymentOfflineCollectionVoucherMapper.class);
        contract("pay_offline_bank_statement_batch", "payment_offline_bank_statement_batch",
                PaymentOfflineBankStatementBatchEntity.class, PaymentOfflineBankStatementBatchMapper.class);
        contract("pay_offline_bank_statement_item", "payment_offline_bank_statement_item",
                PaymentOfflineBankStatementItemEntity.class, PaymentOfflineBankStatementItemMapper.class);
        contract("pay_offline_collection_match", "payment_offline_collection_match",
                PaymentOfflineCollectionMatchEntity.class, PaymentOfflineCollectionMatchMapper.class);
        contract("pay_offline_refund_process", "payment_offline_refund_process",
                PaymentOfflineRefundEntity.class, PaymentOfflineRefundMapper.class);
        contract("pay_channel_bill_batch", "payment_channel_bill_batch",
                PaymentChannelBillBatchEntity.class, PaymentChannelBillBatchMapper.class);
        contract("pay_channel_bill_detail", "payment_channel_bill_detail",
                PaymentChannelBillDetailEntity.class, PaymentChannelBillDetailMapper.class);
        contract("pay_reconcile_diff", "payment_difference", PaymentDifferenceEntity.class, PaymentDifferenceMapper.class);
        contract("pay_settlement_summary", "payment_settlement_summary",
                PaymentSettlementSummaryEntity.class, PaymentSettlementSummaryMapper.class);
        contract("pay_notify_record", "payment_notification_record",
                PaymentNotificationRecordEntity.class, PaymentNotificationRecordMapper.class);
        contract("pay_operation_audit", "payment_operation_audit",
                PaymentOperationAudit.class, PaymentOperationAuditMapper.class);
        contract("pay_risk_rule", "payment_risk_rule", PaymentRiskRuleEntity.class, PaymentRiskRuleMapper.class);
    }

    @Test
    @DisplayName("design core tables should have physical table, auditable entity and MyBatis-Plus mapper")
    void designCoreTables_shouldHavePhysicalTableEntityAndMapper() throws IOException {
        List<String> designTables = designCoreTables();
        assertThat(CONTRACTS.keySet()).containsExactlyElementsOf(designTables);

        String migrations = migrations();
        for (Map.Entry<String, ModelContract> entry : CONTRACTS.entrySet()) {
            ModelContract contract = entry.getValue();
            assertThat(migrations)
                    .as(entry.getKey() + " physical table " + contract.tableName())
                    .contains("`" + contract.tableName() + "`");
            assertThat(contract.entityClass())
                    .as(entry.getKey() + " entity extends AuditableEntity")
                    .isAssignableTo(AuditableEntity.class);
            assertThat(contract.entityClass().getAnnotation(TableName.class).value())
                    .as(entry.getKey() + " entity table")
                    .isEqualTo(contract.tableName());
            assertThat(mapperEntityType(contract.mapperClass()))
                    .as(entry.getKey() + " mapper generic entity")
                    .isEqualTo(contract.entityClass());
        }
    }

    private static void contract(
            String designTable,
            String tableName,
            Class<? extends AuditableEntity> entityClass,
            Class<? extends BaseMapper<?>> mapperClass) {
        CONTRACTS.put(designTable, new ModelContract(tableName, entityClass, mapperClass));
    }

    private List<String> designCoreTables() throws IOException {
        String design = paymentDesign();
        int sectionStart = design.indexOf("### 11.1");
        int sectionEnd = design.indexOf("### 11.2", sectionStart);
        assertThat(sectionStart).isGreaterThanOrEqualTo(0);
        assertThat(sectionEnd).isGreaterThan(sectionStart);
        Matcher matcher = DESIGN_TABLE_PATTERN.matcher(design.substring(sectionStart, sectionEnd));
        return matcher.results()
                .map(result -> result.group(1))
                .toList();
    }

    private String paymentDesign() throws IOException {
        try (var files = Files.list(Path.of(DESIGN_DIR))) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".md")).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (content.contains("pay_channel_contract_value") && content.contains("pay_risk_rule")) {
                    return content;
                }
            }
        }
        throw new IllegalStateException("Payment design document with core payment tables not found");
    }

    private String migrations() throws IOException {
        StringBuilder content = new StringBuilder();
        try (var files = Files.list(Path.of(MIGRATION_DIR))) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".sql")).toList()) {
                content.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return content.toString();
    }

    private Class<?> mapperEntityType(Class<?> mapperClass) {
        for (Type type : mapperClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType().equals(BaseMapper.class)) {
                Type argument = parameterizedType.getActualTypeArguments()[0];
                assertThat(argument).isInstanceOf(Class.class);
                return (Class<?>) argument;
            }
        }
        throw new IllegalStateException("Mapper does not extend BaseMapper directly: " + mapperClass.getName());
    }

    private record ModelContract(
            String tableName,
            Class<? extends AuditableEntity> entityClass,
            Class<? extends BaseMapper<?>> mapperClass) {
    }
}
