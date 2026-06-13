package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTenantIsolationContractTest {

    private static final String TENANT_CONDITION = "tenant_id = #{tenantId}";
    private static final Pattern TENANT_IGNORE = Pattern.compile(
            "@InterceptorIgnore\\(tenantLine\\s*=\\s*\"true\"\\)");
    private static final Pattern METHOD_NAME = Pattern.compile("(\\w+)\\s*\\(");
    private static final List<MapperContract> CONTRACTS = List.of(
            contract("PaymentBusinessOrderMapper", "countBusinessOrders", true),
            contract("PaymentBusinessOrderMapper", "selectBusinessOrderPage", true),
            contract("PaymentBusinessOrderMapper", "selectBusinessOrderDetail", true),
            contract("PaymentBusinessOrderMapper", "closeOpenBusinessOrder", true),
            contract("PaymentOrderMapper", "countPaymentOrders", true),
            contract("PaymentOrderMapper", "countPaymentOrdersByStatus", true),
            contract("PaymentOrderMapper", "countProcessingPaymentBacklog", true),
            contract("PaymentOrderMapper", "selectPaymentOrderPage", true),
            contract("PaymentOrderMapper", "selectPaymentOrderDetail", true),
            contract("PaymentOrderMapper", "selectPaymentOrderById", true),
            contract("PaymentOrderMapper", "selectOpenPaymentOrder", true),
            contract("PaymentOrderMapper", "selectExpiredOpenPaymentOrders", true),
            contract("PaymentOrderMapper", "selectProcessingPaymentOrders", true),
            contract("PaymentOrderMapper", "selectSuccessfulOpenPaymentOrder", true),
            contract("PaymentOrderMapper", "lockSuccessfulOpenPaymentOrder", true),
            contract("PaymentOrderMapper", "selectLatestFlowNo", true),
            contract("PaymentOrderMapper", "selectByTenantAndChannelTradeNo", true),
            contract("PaymentOrderMapper", "selectByTenantAndPayOrderNo", true),
            contract("PaymentOrderMapper", "selectByPayOrderNo", "where po.pay_order_no = #{payOrderNo}", "limit 1"),
            contract("PaymentOrderMapper", "selectEntityByTenantAndId", true),
            contract("PaymentOrderMapper", "selectChannelFailureMetrics", true),
            contract("PaymentOrderMapper", "selectSuccessfulChannelOrdersMissingInBill", true),
            contract("PaymentOrderMapper", "selectSuccessfulChannelOrdersForBill", true),
            contract("PaymentOrderMapper", "updatePayingQueryResult", true),
            contract("PaymentOrderMapper", "updateCreatedApplyResult", true),
            contract("PaymentOrderMapper", "markCreatedApplyFailed", true),
            contract("PaymentOrderMapper", "updatePayingCallbackResult", true),
            contract("PaymentOrderMapper", "updateOfflineCollectionSuccess", true),
            contract("PaymentOrderMapper", "markDuplicatePaymentSuccess", true),
            contract("PaymentOrderMapper", "markDuplicatePaymentRefunding", true),
            contract("PaymentOrderMapper", "markDuplicatePaymentRefunded", true),
            contract("PaymentOrderMapper", "closeOpenPaymentOrder", true),
            contract("PaymentOrderStatusFlowMapper", "selectStatusFlows", true),
            contract("PaymentRefundOrderMapper", "countRefundOrders", true),
            contract("PaymentRefundOrderMapper", "countRefundOrdersByStatus", true),
            contract("PaymentRefundOrderMapper", "selectRefundOrderPage", true),
            contract("PaymentRefundOrderMapper", "selectRefundOrderDetail", true),
            contract("PaymentRefundOrderMapper", "selectOpenRefundOrder", true),
            contract("PaymentRefundOrderMapper", "selectByTenantAndRefundOrderNo", true),
            contract("PaymentRefundOrderMapper", "selectByTenantAndChannelRefundNo", true),
            contract("PaymentRefundOrderMapper", "updateRefundingQueryResult", true),
            contract("PaymentRefundOrderMapper", "sumOccupyingRefundAmount", true),
            contract("PaymentRefundOrderMapper", "selectLatestFlowNo", true),
            contract("PaymentRefundOrderMapper", "selectEntityByTenantAndChannelRefundNo", true),
            contract("PaymentRefundOrderMapper", "selectSuccessfulChannelRefundsMissingInBill", true),
            contract("PaymentRefundOrderMapper", "selectSuccessfulChannelRefundsForBill", true),
            contract("PaymentTransactionFlowMapper", "countTransactionFlows", true),
            contract("PaymentTransactionFlowMapper", "selectTransactionFlowPage", true),
            contract("PaymentTransactionFlowMapper", "selectTransactionFlowDetail", true),
            contract("PaymentTransactionFlowMapper", "selectChannelFeeFlowByPaymentOrder", true),
            contract("PaymentTransactionFlowMapper", "selectChannelFeeFlowByRefundOrder", true),
            contract("PaymentExceptionOrderMapper", "countExceptionOrders", true),
            contract("PaymentExceptionOrderMapper", "countCallbackFailureExceptionOrders", true),
            contract("PaymentExceptionOrderMapper", "selectExceptionOrderPage", true),
            contract("PaymentExceptionOrderMapper", "selectExceptionOrderDetail", true),
            contract("PaymentExceptionOrderMapper", "selectActiveByBusinessKey", true),
            contract("PaymentExceptionOrderMapper", "handleExceptionOrder", true),
            contract("PaymentNotificationRecordMapper", "countNotificationRecords", true),
            contract("PaymentNotificationRecordMapper", "countFailedNotificationRecords", true),
            contract("PaymentNotificationRecordMapper", "selectNotificationRecordPage", true),
            contract("PaymentNotificationRecordMapper", "selectNotificationRecordDetail", true),
            contract("PaymentNotificationRecordMapper", "selectDueNotificationRecords", true),
            contract("PaymentNotificationRecordMapper", "selectDueNotificationTenantIds", false),
            contract("PaymentNotificationRecordMapper", "claimDueNotificationRecord", true),
            contract("PaymentNotificationRecordMapper", "updateDeliveryResult", true),
            contract("PaymentNotificationRecordMapper", "manualRetryNotificationRecord", true),
            contract("PaymentReconciliationMapper", "countReconciliations", true),
            contract("PaymentReconciliationMapper", "selectReconciliationPage", true),
            contract("PaymentReconciliationMapper", "selectReconciliationDetail", true),
            contract("PaymentReconciliationMapper", "countImportedFile", true),
            contract("PaymentReconciliationMapper", "selectMangoPayBillItems", true),
            contract("PaymentDifferenceMapper", "countDifferences", true),
            contract("PaymentDifferenceMapper", "selectDifferencePage", true),
            contract("PaymentDifferenceMapper", "selectDifferenceDetail", true),
            contract("PaymentDifferenceMapper", "handleDifference", true),
            contract("PaymentSettlementSummaryMapper", "countSettlementSummaries", true),
            contract("PaymentSettlementSummaryMapper", "selectSettlementSummaryPage", true),
            contract("PaymentSettlementSummaryMapper", "selectSettlementSummaryDetail", true),
            contract("PaymentSettlementSummaryMapper", "selectByScope", true),
            contract("PaymentSettlementSummaryMapper", "selectSettlementCalculation", true),
            contract("PaymentSettlementSummaryMapper", "countCompletedReconciliation", true),
            contract("PaymentSettlementSummaryMapper", "selectUnresolvedDifferenceCalculation", true),
            contract("PaymentSettlementSummaryMapper", "confirmGeneratedSummary", true),
            contract("PaymentSettlementSummaryMapper", "voidConfirmedSummary", true),
            contract("PaymentOperationAuditMapper", "countOperationAudits", true),
            contract("PaymentOperationAuditMapper", "selectOperationAuditPage", true),
            contract("PaymentOfflineCollectionMapper", "countOfflineCollections", true),
            contract("PaymentOfflineCollectionMapper", "selectOfflineCollectionPage", true),
            contract("PaymentOfflineCollectionMapper", "selectOfflineCollectionDetail", true),
            contract("PaymentOfflineCollectionMapper", "selectByPayOrderNoForUpdate", true),
            contract("PaymentOfflineCollectionMapper", "selectEntityForUpdate", true),
            contract("PaymentOfflineCollectionMapper", "selectByReconciliationCodeForUpdate", true),
            contract("PaymentOfflineCollectionMapper", "submitTransferVoucher", true),
            contract("PaymentOfflineCollectionMapper", "confirmCollection", true),
            contract("PaymentOfflineBankStatementBatchMapper", "countBatches", true),
            contract("PaymentOfflineBankStatementBatchMapper", "selectBatchPage", true),
            contract("PaymentOfflineBankStatementBatchMapper", "selectBatchDetail", true),
            contract("PaymentOfflineBankStatementBatchMapper", "countImportedFile", true),
            contract("PaymentOfflineBankStatementBatchMapper", "refreshSummary", true),
            contract("PaymentOfflineBankStatementItemMapper", "selectItemsByBatch", true),
            contract("PaymentOfflineBankStatementItemMapper", "selectEntityForUpdate", true),
            contract("PaymentOfflineBankStatementItemMapper", "countExistingStatement", true),
            contract("PaymentOfflineBankStatementItemMapper", "markConfirmed", true),
            contract("PaymentOfflineCollectionMatchMapper", "markConfirmed", true),
            contract("PaymentOfflineRefundMapper", "countOfflineRefunds", true),
            contract("PaymentOfflineRefundMapper", "selectOfflineRefundPage", true),
            contract("PaymentOfflineRefundMapper", "selectOfflineRefundDetail", true),
            contract("PaymentOfflineRefundMapper", "sumRefundedAmountByCollection", true),
            contract("PaymentRefundApprovalMapper", "countRefundApprovals", true),
            contract("PaymentRefundApprovalMapper", "selectRefundApprovalPage", true),
            contract("PaymentRefundApprovalMapper", "selectRefundApprovalDetail", true),
            contract("PaymentRefundApprovalMapper", "selectEntityForUpdate", true),
            contract("PaymentRefundApprovalMapper", "selectEntityByApprovalNoForUpdate", true),
            contract("PaymentRefundApprovalMapper", "sumPendingApprovalAmount", true),
            contract("PaymentChannelQueryRecordMapper", "countByTenantAndPayOrderNo", true),
            contract("PaymentChannelQueryRecordMapper", "selectLastByTenantAndPayOrderNo", true),
            contract("PaymentRefundQueryRecordMapper", "countByTenantAndRefundOrderNo", true),
            contract("PaymentRefundQueryRecordMapper", "selectLastByTenantAndRefundOrderNo", true),
            contract("PaymentCashierConfigMapper", "selectByIdIgnoreTenant", "where id = #{id}", "and del_flag = 0"),
            contract("PaymentMangoPayScenarioControlMapper", "selectNextActive", true),
            contract("PaymentMangoPayScenarioControlMapper", "consume", true),
            contract("PaymentChannelBillDetailMapper", "selectBillDetails", true)
    );

    @Test
    @DisplayName("tenant ignored payment mapper statements should keep explicit tenant boundaries")
    void tenantIgnoredMapperStatements_keepExplicitTenantBoundaries() throws IOException {
        assertThat(contractKeys()).containsExactlyElementsOf(tenantIgnoredMapperMethodKeys());
        for (MapperContract contract : CONTRACTS) {
            String statement = statement(contract.mapperName(), contract.statementId());
            if (contract.requiredSnippets().length > 0) {
                for (String requiredSnippet : contract.requiredSnippets()) {
                    assertThat(statement)
                            .as(contract.mapperName() + "." + contract.statementId())
                            .contains(requiredSnippet);
                }
            } else if (contract.requiresTenantCondition()) {
                assertThat(statement)
                        .as(contract.mapperName() + "." + contract.statementId())
                        .contains(TENANT_CONDITION);
            } else {
                assertThat(statement)
                        .as(contract.mapperName() + "." + contract.statementId())
                        .contains("group by nr.tenant_id");
            }
        }
    }

    private Set<String> contractKeys() {
        Set<String> keys = new TreeSet<>();
        CONTRACTS.forEach(contract -> keys.add(contract.key()));
        return keys;
    }

    private static MapperContract contract(String mapperName, String statementId, boolean requiresTenantCondition) {
        return new MapperContract(mapperName, statementId, requiresTenantCondition, new String[0]);
    }

    private static MapperContract contract(String mapperName, String statementId, String... requiredSnippets) {
        return new MapperContract(mapperName, statementId, false, requiredSnippets);
    }

    private Set<String> tenantIgnoredMapperMethodKeys() throws IOException {
        Set<String> keys = new TreeSet<>();
        Path mapperDir = Path.of("src/main/java/io/mango/payment/core/mapper");
        try (var files = Files.list(mapperDir)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Mapper.java")).toList()) {
                String mapperName = file.getFileName().toString().replace(".java", "");
                String java = Files.readString(file, StandardCharsets.UTF_8);
                collectTenantIgnoredMethods(keys, mapperName, java);
            }
        }
        return keys;
    }

    private void collectTenantIgnoredMethods(Set<String> keys, String mapperName, String java) {
        Matcher matcher = TENANT_IGNORE.matcher(java);
        while (matcher.find()) {
            int declarationEnd = java.indexOf(';', matcher.end());
            assertThat(declarationEnd).as(mapperName + " tenant ignored method declaration").isGreaterThan(matcher.end());
            String declaration = java.substring(matcher.end(), declarationEnd);
            Matcher methodMatcher = METHOD_NAME.matcher(declaration);
            String methodName = null;
            if (methodMatcher.find()) {
                methodName = methodMatcher.group(1);
            }
            assertThat(methodName).as(mapperName + " tenant ignored method name").isNotNull();
            keys.add(mapperName + "." + methodName);
        }
    }

    private String statement(String mapperName, String statementId) throws IOException {
        String xml = resource("/mapper/payment/" + mapperName + ".xml");
        String start = "id=\"" + statementId + "\"";
        int idIndex = xml.indexOf(start);
        assertThat(idIndex).as(mapperName + "." + statementId + " exists").isGreaterThanOrEqualTo(0);
        int tagStart = xml.lastIndexOf('<', idIndex);
        int tagEnd = xml.indexOf('>', idIndex);
        String tagName = xml.substring(tagStart + 1, tagEnd).split("\\s+", 2)[0];
        int statementEnd = xml.indexOf("</" + tagName + ">", tagEnd);
        assertThat(statementEnd).as(mapperName + "." + statementId + " closing tag exists").isGreaterThan(tagEnd);
        return xml.substring(tagStart, statementEnd) + "\n" + sqlFragments(xml);
    }

    private String sqlFragments(String xml) {
        StringBuilder fragments = new StringBuilder();
        int searchFrom = 0;
        while (true) {
            int sqlStart = xml.indexOf("<sql ", searchFrom);
            if (sqlStart < 0) {
                return fragments.toString();
            }
            int sqlEnd = xml.indexOf("</sql>", sqlStart);
            assertThat(sqlEnd).as("sql fragment closing tag exists").isGreaterThan(sqlStart);
            fragments.append(xml, sqlStart, sqlEnd);
            searchFrom = sqlEnd + "</sql>".length();
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record MapperContract(
            String mapperName,
            String statementId,
            boolean requiresTenantCondition,
            String[] requiredSnippets) {
        private String key() {
            return mapperName + "." + statementId;
        }
    }
}
