package io.mango.payment.starter.resource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import io.mango.payment.core.entity.PaymentBaseEntity;
import io.mango.payment.core.entity.PaymentCashierConfigEntity;
import io.mango.payment.core.entity.PaymentChannelCapabilityEntity;
import io.mango.payment.core.entity.PaymentChannelContractCapabilityEntity;
import io.mango.payment.core.entity.PaymentChannelContractEntity;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.entity.PaymentChannelEntity;
import io.mango.payment.core.entity.PaymentChannelFieldTemplateEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubjectEntity;
import io.mango.payment.core.entity.PaymentMethodCategoryEntity;
import io.mango.payment.core.entity.PaymentMethodEntity;
import io.mango.payment.core.entity.PaymentMethodRouteRuleEntity;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItemEntity;
import io.mango.payment.core.entity.PaymentRiskRuleEntity;
import io.mango.payment.core.entity.PaymentSubjectBankAccountEntity;
import io.mango.payment.core.entity.PaymentTenantEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
import io.mango.payment.core.mapper.PaymentChannelFieldTemplateMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodCategoryMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleItemMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleMapper;
import io.mango.payment.core.mapper.PaymentRiskRuleMapper;
import io.mango.payment.core.mapper.PaymentSubjectBankAccountMapper;
import io.mango.payment.core.mapper.PaymentTenantMapper;
import io.mango.payment.core.service.PaymentSensitiveValueCodec;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

import static io.mango.payment.starter.resource.PaymentResourceTypes.APPLICATION;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CASHIER_CONFIG;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL_CAPABILITY;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL_CONTRACT;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL_CONTRACT_CAPABILITY;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL_CONTRACT_VALUE;
import static io.mango.payment.starter.resource.PaymentResourceTypes.CHANNEL_FIELD_TEMPLATE;
import static io.mango.payment.starter.resource.PaymentResourceTypes.ENTERPRISE_SUBJECT;
import static io.mango.payment.starter.resource.PaymentResourceTypes.METHOD;
import static io.mango.payment.starter.resource.PaymentResourceTypes.METHOD_CATEGORY;
import static io.mango.payment.starter.resource.PaymentResourceTypes.METHOD_ROUTE_RULE;
import static io.mango.payment.starter.resource.PaymentResourceTypes.METHOD_ROUTE_RULE_ITEM;
import static io.mango.payment.starter.resource.PaymentResourceTypes.RISK_RULE;
import static io.mango.payment.starter.resource.PaymentResourceTypes.SUBJECT_BANK_ACCOUNT;
import static io.mango.payment.starter.resource.PaymentResourceTypes.TENANT;

/**
 * Payment-owned Resource Registry handlers.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentResourceHandlerConfiguration {

    private static final String FUIOU_DEMO_CONTRACT_BIZ_KEY =
            "payment.channel-contract.FUIOU_PAY_MANGO_TECH";
    private static final Set<String> FUIOU_DEMO_SECRET_FIELDS = Set.of("privateKey", "gatewayMerchantKey");
    private static final Set<String> PAYMENT_SECRET_CONFIG_FIELDS = Set.of(
            "appsecret", "apisecret", "privatekey", "merchantkey", "gatewaymerchantkey", "mchntkey");

    @Bean
    ResourceHandler paymentMethodCategoryResourceHandler(PaymentMethodCategoryMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentMethodCategoryEntity.class, METHOD_CATEGORY, "payment_method_category",
                fields("targetId", "categoryCode", "categoryName", "level", "parentId", "sort", "status", "tenantId"),
                required("targetId", "categoryCode", "categoryName", "level", "sort", "status", "tenantId"),
                List.of(), true);
    }

    @Bean
    ResourceHandler paymentMethodResourceHandler(PaymentMethodMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentMethodEntity.class, METHOD, "payment_method",
                fields("targetId", "methodCode", "methodName", "sort", "status", "tenantId", "accountNature",
                        "instrumentType", "interactionType", "terminalScope", "paymentMaterialType", "cashierGroupCode",
                        "cashierGroupName", "cashierGroupSort", "requiresBankSelection", "requiresQrRefresh", "description"),
                required("targetId", "methodCode", "methodName", "sort", "status", "accountNature", "instrumentType",
                        "interactionType", "terminalScope", "paymentMaterialType"),
                List.of(METHOD_CATEGORY), true);
    }

    @Bean
    ResourceHandler paymentChannelResourceHandler(PaymentChannelMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentChannelEntity.class, CHANNEL, "payment_channel",
                fields("targetId", "channelCode", "channelName", "environment", "status", "tenantId",
                        "channelType", "adapterType", "gatewayBaseUrl", "fieldTemplateJson", "capabilitySummary",
                        "billFetchModes"),
                required("targetId", "channelCode", "channelName", "environment", "status", "tenantId", "channelType",
                        "adapterType"), List.of(), true);
    }

    @Bean
    ResourceHandler paymentChannelFieldTemplateResourceHandler(
            PaymentChannelFieldTemplateMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentChannelFieldTemplateEntity.class,
                CHANNEL_FIELD_TEMPLATE, "payment_channel_field_template",
                fields("targetId", "channelId", "fieldCode", "fieldLabel", "componentType", "dataType", "requiredFlag",
                        "sensitiveFlag", "encryptedFlag", "maskedFlag", "fileReferenceFlag", "optionJson", "validationJson",
                        "fieldGroup", "sort", "status", "tenantId"),
                required("targetId", "channelId", "fieldCode", "fieldLabel", "componentType", "dataType", "requiredFlag",
                        "sensitiveFlag", "encryptedFlag", "maskedFlag", "fileReferenceFlag", "sort", "status", "tenantId"),
                List.of(CHANNEL), true);
    }

    @Bean
    ResourceHandler paymentChannelCapabilityResourceHandler(
            PaymentChannelCapabilityMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentChannelCapabilityEntity.class,
                CHANNEL_CAPABILITY, "payment_channel_capability",
                fields("targetId", "channelId", "methodCode", "terminalType", "environment", "supportsRefund",
                        "supportsQuery", "supportsClose", "supportsBill", "supportsReconcile", "minAmount", "maxAmount",
                        "status", "tenantId"),
                required("targetId", "channelId", "methodCode", "terminalType", "environment", "supportsRefund",
                        "supportsQuery", "supportsClose", "supportsBill", "supportsReconcile", "status", "tenantId"),
                List.of(CHANNEL, METHOD), true);
    }

    @Bean
    ResourceHandler paymentRiskRuleResourceHandler(PaymentRiskRuleMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentRiskRuleEntity.class, RISK_RULE, "payment_risk_rule",
                fields("targetId", "ruleCode", "ruleName", "ruleScope", "appId", "subjectId", "methodCode", "riskType",
                        "thresholdAmount", "periodType", "periodLimitCount", "periodLimitAmount", "actionType", "priority",
                        "status", "tenantId"),
                required("targetId", "ruleCode", "ruleName", "ruleScope", "riskType", "actionType", "priority", "status",
                        "tenantId"), List.of(METHOD), true);
    }

    @Bean
    ResourceHandler paymentTenantResourceHandler(PaymentTenantMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentTenantEntity.class, TENANT, "payment_tenant",
                fields("targetId", "tenantCode", "tenantName", "platformTenantId", "status", "tenantId"),
                required("targetId", "tenantCode", "tenantName", "platformTenantId", "status", "tenantId"),
                List.of(), true);
    }

    @Bean
    ResourceHandler paymentApplicationResourceHandler(PaymentApplicationMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentApplicationEntity.class, APPLICATION, "payment_application",
                fields("targetId", "appId", "appName", "status", "tenantId", "signAlgorithm", "ipWhitelist",
                        "payloadEncryptEnabled", "secretConfigured", "secretVersion", "secretLastResetTime",
                        "notifyRetryPolicy", "demoApp", "ipWhitelistEnabled"),
                required("targetId", "appId", "appName", "status", "tenantId", "payloadEncryptEnabled",
                        "secretConfigured", "secretVersion", "demoApp", "ipWhitelistEnabled"),
                List.of(TENANT), true);
    }

    @Bean
    ResourceHandler paymentEnterpriseSubjectResourceHandler(
            PaymentEnterpriseSubjectMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentEnterpriseSubjectEntity.class,
                ENTERPRISE_SUBJECT, "payment_enterprise_subject",
                fields("targetId", "subjectName", "creditCode", "creditCodeHash", "bankAccountNo", "bankName",
                        "licenseFileId", "status", "tenantId"),
                required("targetId", "subjectName", "creditCode", "status", "tenantId"),
                List.of(TENANT), true);
    }

    @Bean
    ResourceHandler paymentSubjectBankAccountResourceHandler(
            PaymentSubjectBankAccountMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentSubjectBankAccountEntity.class,
                SUBJECT_BANK_ACCOUNT, "payment_subject_bank_account",
                fields("targetId", "subjectId", "accountName", "accountNo", "bankName", "bankBranchName", "bankCode",
                        "accountType", "defaultAccount", "status", "tenantId"),
                required("targetId", "subjectId", "accountName", "accountNo", "bankName", "accountType", "defaultAccount",
                        "status", "tenantId"), List.of(ENTERPRISE_SUBJECT), true);
    }

    @Bean
    ResourceHandler paymentCashierConfigResourceHandler(PaymentCashierConfigMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentCashierConfigEntity.class, CASHIER_CONFIG, "payment_cashier_config",
                fields("targetId", "cashierName", "applicationId", "resultReturnUrl", "status", "tenantId", "methodCodes",
                        "defaultMethodCode", "methodDisplayOrder", "displayConfig", "defaultCashier",
                        "enterpriseSubjectIds"),
                required("targetId", "cashierName", "applicationId", "status", "tenantId", "defaultCashier"),
                List.of(APPLICATION, ENTERPRISE_SUBJECT, METHOD), true);
    }

    @Bean
    ResourceHandler paymentChannelContractResourceHandler(
            PaymentChannelContractMapper mapper,
            ObjectMapper objectMapper,
            PaymentSensitiveValueCodec sensitiveValueCodec) {
        return new PaymentTableResourceHandler<>(mapper, objectMapper,
                new PaymentTableResourceHandler.Definition<>(
                        CHANNEL_CONTRACT,
                        "payment_channel_contract",
                        PaymentChannelContractEntity.class,
                        fields("targetId", "contractCode", "contractName", "subjectId", "channelId", "environment",
                                "merchantNo", "appId", "configValuesJson", "enabledMethodCodes", "status", "tenantId"),
                        required("targetId", "contractCode", "contractName", "subjectId", "channelId", "environment",
                                "status", "tenantId"),
                        List.of(ENTERPRISE_SUBJECT, CHANNEL),
                        statusColumn(true),
                        null,
                        (resource, configValuesJson) -> protectDemoContractSecrets(
                                resource, configValuesJson, objectMapper, sensitiveValueCodec)));
    }

    @Bean
    ResourceHandler paymentChannelContractValueResourceHandler(
            PaymentChannelContractValueMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentChannelContractValueEntity.class,
                CHANNEL_CONTRACT_VALUE, "payment_channel_contract_value",
                fields("targetId", "contractId", "fieldCode", "valueText", "fileId", "valueSource", "sensitiveFlag",
                        "tenantId"),
                required("targetId", "contractId", "fieldCode", "valueSource", "sensitiveFlag", "tenantId"),
                List.of(CHANNEL_CONTRACT), false, mapper::deletePhysicallyById);
    }

    @Bean
    ResourceHandler paymentChannelContractCapabilityResourceHandler(
            PaymentChannelContractCapabilityMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentChannelContractCapabilityEntity.class, CHANNEL_CONTRACT_CAPABILITY,
                "payment_channel_contract_capability",
                fields("targetId", "contractId", "channelCapabilityId", "methodCode", "terminalType", "feeRate",
                        "minAmount", "maxAmount", "priority", "certificateExpireTime", "status", "tenantId"),
                required("targetId", "contractId", "channelCapabilityId", "methodCode", "terminalType", "priority", "status",
                        "tenantId"), List.of(CHANNEL_CONTRACT, CHANNEL_CAPABILITY), true);
    }

    @Bean
    ResourceHandler paymentMethodRouteRuleResourceHandler(
            PaymentMethodRouteRuleMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentMethodRouteRuleEntity.class,
                METHOD_ROUTE_RULE, "payment_method_route_rule",
                fields("targetId", "ruleCode", "ruleName", "appId", "subjectId", "methodCode", "terminalType",
                        "environment", "routeMode", "fallbackEnabled", "status", "tenantId"),
                required("targetId", "ruleCode", "ruleName", "appId", "subjectId", "methodCode", "terminalType",
                        "environment", "routeMode", "fallbackEnabled", "status", "tenantId"),
                List.of(APPLICATION, ENTERPRISE_SUBJECT, METHOD), true);
    }

    @Bean
    ResourceHandler paymentMethodRouteRuleItemResourceHandler(
            PaymentMethodRouteRuleItemMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, PaymentMethodRouteRuleItemEntity.class,
                METHOD_ROUTE_RULE_ITEM, "payment_method_route_rule_item",
                fields("targetId", "ruleId", "contractCapabilityId", "priority", "weight", "minAmount", "maxAmount",
                        "status", "tenantId"),
                required("targetId", "ruleId", "contractCapabilityId", "priority", "weight", "status", "tenantId"),
                List.of(METHOD_ROUTE_RULE, CHANNEL_CONTRACT_CAPABILITY), true);
    }

    private <E extends PaymentBaseEntity> ResourceHandler handler(
            BaseMapper<E> mapper,
            ObjectMapper objectMapper,
            Class<E> entityType,
            String resourceType,
            String table,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies,
            boolean hasStatus) {
        return handler(mapper, objectMapper, entityType, resourceType, table, fields, requiredFields, dependencies,
                hasStatus, null);
    }

    private <E extends PaymentBaseEntity> ResourceHandler handler(
            BaseMapper<E> mapper,
            ObjectMapper objectMapper,
            Class<E> entityType,
            String resourceType,
            String table,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies,
            boolean hasStatus,
            LongConsumer physicalDelete) {
        return new PaymentTableResourceHandler<>(mapper, objectMapper,
                new PaymentTableResourceHandler.Definition<>(
                        resourceType,
                        table,
                        entityType,
                        fields,
                        requiredFields,
                        dependencies,
                        statusColumn(hasStatus),
                        physicalDelete));
    }

    private static Map<String, String> fields(String... fieldNames) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            if ("targetId".equals(fieldName)) {
                fields.put(fieldName, "id");
            } else {
                fields.put(fieldName, snakeCase(fieldName));
            }
        }
        return fields;
    }

    private static String statusColumn(boolean hasStatus) {
        if (hasStatus) {
            return "status";
        }
        return null;
    }

    private static Set<String> required(String... fieldNames) {
        return Set.of(fieldNames);
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    private static String protectDemoContractSecrets(
            ResourceDeclaration resource,
            String configValuesJson,
            ObjectMapper objectMapper,
            PaymentSensitiveValueCodec sensitiveValueCodec) {
        try {
            JsonNode root = objectMapper.readTree(configValuesJson);
            if (!(root instanceof ObjectNode values)) {
                throw new IllegalArgumentException("Payment configValuesJson must be an object");
            }
            Set<String> declaredSecretFields = new java.util.LinkedHashSet<>();
            collectDeclaredSecretFields(values, declaredSecretFields);
            if (declaredSecretFields.isEmpty()) {
                return configValuesJson;
            }
            if (!FUIOU_DEMO_CONTRACT_BIZ_KEY.equals(resource.getBizKey())
                    || !FUIOU_DEMO_SECRET_FIELDS.containsAll(declaredSecretFields)) {
                throw new IllegalArgumentException(
                        "Payment resource must not contain merchant secrets: " + declaredSecretFields);
            }
            declaredSecretFields.forEach(field -> {
                JsonNode secretValue = values.get(field);
                if (secretValue == null || !secretValue.isTextual() || secretValue.asText().isBlank()) {
                    throw new IllegalArgumentException(
                            "Payment demo secret must be a non-blank top-level text field: " + field);
                }
                values.put(field, sensitiveValueCodec.encrypt(secretValue.asText()));
            });
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid payment configValuesJson", ex);
        }
    }

    private static void collectDeclaredSecretFields(JsonNode node, Set<String> secretFields) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (PAYMENT_SECRET_CONFIG_FIELDS.contains(
                        entry.getKey().toLowerCase(java.util.Locale.ROOT))) {
                    secretFields.add(entry.getKey());
                }
                collectDeclaredSecretFields(entry.getValue(), secretFields);
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectDeclaredSecretFields(child, secretFields));
        }
    }
}
