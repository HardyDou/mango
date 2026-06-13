package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.payment.api.PaymentBusinessOrderApi;
import io.mango.payment.api.PaymentDifferenceApi;
import io.mango.payment.api.PaymentExceptionOrderApi;
import io.mango.payment.api.PaymentNotificationRecordApi;
import io.mango.payment.api.PaymentObservabilityApi;
import io.mango.payment.api.PaymentOfflineCollectionApi;
import io.mango.payment.api.PaymentOfflineRefundApi;
import io.mango.payment.api.PaymentOperationAuditApi;
import io.mango.payment.api.PaymentOrderApi;
import io.mango.payment.api.PaymentReconciliationApi;
import io.mango.payment.api.PaymentRefundApprovalApi;
import io.mango.payment.api.PaymentRefundOrderApi;
import io.mango.payment.api.PaymentSecurityApi;
import io.mango.payment.api.PaymentSettlementSummaryApi;
import io.mango.payment.api.PaymentTaskApi;
import io.mango.payment.api.PaymentTransactionFlowApi;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainControllerContractTest {

    @Test
    @DisplayName("payment admin controllers should be split by business domain and implement api contracts")
    void paymentAdminControllers_areSplitByBusinessDomainAndImplementApiContracts() {
        assertThat(PaymentBusinessOrderApi.class).isAssignableFrom(PaymentBusinessOrderController.class);
        assertThat(PaymentOrderApi.class).isAssignableFrom(PaymentOrderController.class);
        assertThat(PaymentOfflineCollectionApi.class).isAssignableFrom(PaymentOfflineCollectionController.class);
        assertThat(PaymentOfflineRefundApi.class).isAssignableFrom(PaymentOfflineRefundController.class);
        assertThat(PaymentRefundOrderApi.class).isAssignableFrom(PaymentRefundOrderController.class);
        assertThat(PaymentRefundApprovalApi.class).isAssignableFrom(PaymentRefundApprovalController.class);
        assertThat(PaymentTransactionFlowApi.class).isAssignableFrom(PaymentTransactionFlowController.class);
        assertThat(PaymentExceptionOrderApi.class).isAssignableFrom(PaymentExceptionOrderController.class);
        assertThat(PaymentNotificationRecordApi.class).isAssignableFrom(PaymentNotificationRecordController.class);
        assertThat(PaymentTaskApi.class).isAssignableFrom(PaymentTaskController.class);
        assertThat(PaymentReconciliationApi.class).isAssignableFrom(PaymentReconciliationController.class);
        assertThat(PaymentDifferenceApi.class).isAssignableFrom(PaymentDifferenceController.class);
        assertThat(PaymentSettlementSummaryApi.class).isAssignableFrom(PaymentSettlementSummaryController.class);
        assertThat(PaymentOperationAuditApi.class).isAssignableFrom(PaymentOperationAuditController.class);
        assertThat(PaymentSecurityApi.class).isAssignableFrom(PaymentSecurityController.class);
        assertThat(PaymentObservabilityApi.class).isAssignableFrom(PaymentObservabilityController.class);
    }

    @Test
    @DisplayName("readonly bucket controller should not exist")
    void readonlyBucketController_shouldNotExist() {
        assertThat(classExists("io.mango.payment.starter.controller.PaymentReadonlyResourceController")).isFalse();
        assertThat(classExists("io.mango.payment.core.service.PaymentReadonlyResourceService")).isFalse();
    }

    @Test
    @DisplayName("transaction flow endpoints should be read only and have no delete mapping")
    void transactionFlowEndpoints_areReadOnly() {
        List<Method> methods = Arrays.stream(PaymentTransactionFlowController.class.getDeclaredMethods()).toList();

        assertThat(methods)
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("pageTransactionFlows", "detailTransactionFlow");
        assertThat(methods)
                .allMatch(method -> method.isAnnotationPresent(GetMapping.class))
                .noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class));
        assertThat(methods)
                .extracting(method -> method.getAnnotation(ApiAccess.class).permission())
                .containsExactlyInAnyOrder("payment:transaction-flow:list", "payment:transaction-flow:query");
    }

    @Test
    @DisplayName("offline collection endpoints should include channel owned operations with dedicated permissions")
    void offlineCollectionEndpoints_includeChannelOwnedOperationsWithDedicatedPermissions() {
        List<Method> methods = Arrays.stream(PaymentOfflineCollectionController.class.getDeclaredMethods())
                .filter(PaymentDomainControllerContractTest::isHttpEndpoint)
                .toList();

        assertThat(methods)
                .extracting(Method::getName)
                .containsExactlyInAnyOrder(
                        "pageOfflineCollections",
                        "detailOfflineCollection",
                        "listOfflineCollectionStatuses",
                        "confirmOfflineCollection",
                        "pageOfflineBankStatements",
                        "detailOfflineBankStatement",
                        "listOfflineBankStatementStatuses",
                        "listOfflineBankStatementMatchStatuses",
                        "importOfflineBankStatement",
                        "confirmOfflineBankStatementMatch",
                        "createOfflineRefund");
        assertThat(methods).noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class));
        assertThat(methods)
                .extracting(method -> method.getAnnotation(ApiAccess.class).permission())
                .containsExactlyInAnyOrder(
                        "payment:offline-collection:list",
                        "payment:offline-collection:query",
                        "payment:offline-collection:list",
                        "payment:offline-collection:confirm",
                        "payment:offline-collection:bank-statement:list",
                        "payment:offline-collection:bank-statement:query",
                        "payment:offline-collection:bank-statement:list",
                        "payment:offline-collection:bank-statement:list",
                        "payment:offline-collection:bank-statement:import",
                        "payment:offline-collection:bank-statement:confirm",
                        "payment:offline-collection:refund");
    }

    @Test
    @DisplayName("payment channel public callback endpoint should not require login token")
    void paymentChannelPublicCallbackEndpoint_shouldNotRequireLoginToken() throws NoSuchMethodException {
        Method method = PaymentChannelCallbackController.class.getDeclaredMethod("handlePublic", String.class, HttpServletRequest.class);

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);

        assertThat(mapping.value())
                .containsExactly("/payment/channel-callbacks/{channelCode}", "/api/payment/channel-callbacks/{channelCode}");
        assertThat(mapping.method()).containsExactly(RequestMethod.POST, RequestMethod.GET);
        assertThat(method.getAnnotation(ApiAccess.class).mode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
    }

    @Test
    @DisplayName("PAY-OPS-001 manual operation endpoints should use dedicated permissions")
    void payOps001ManualOperationEndpoints_useDedicatedPermissions() throws NoSuchMethodException {
        Map<Class<?>, Map<String, String>> expectedPermissions = Map.ofEntries(
                Map.entry(PaymentRefundOrderController.class, Map.of("queryRefundOrder", "payment:refund-order:query-channel")),
                Map.entry(PaymentRefundApprovalController.class, Map.of("createRefundApproval", "payment:refund-approval:create")),
                Map.entry(PaymentExceptionOrderController.class, Map.of(
                        "listExceptionOrderActions", "payment:exception-order:handle",
                        "handleExceptionOrder", "payment:exception-order:handle")),
                Map.entry(PaymentNotificationRecordController.class, Map.of(
                        "retryNotificationRecord", "payment:notification-record:retry",
                        "deliverDueNotificationRecords", "payment:notification-record:deliver-due")),
                Map.entry(PaymentOrderController.class, Map.of("syncPaymentOrderStatus", "payment:payment-order:sync-status")),
                Map.entry(PaymentTaskController.class, Map.of(
                        "expireOpenPaymentOrders", "payment:task:expire-open-orders",
                        "queryProcessingPaymentOrders", "payment:task:query-processing-orders")),
                Map.entry(PaymentReconciliationController.class, Map.of(
                        "importReconciliation", "payment:reconciliation:import",
                        "generateMangoPayVirtualBill", "payment:reconciliation:import")),
                Map.entry(PaymentDifferenceController.class, Map.of(
                        "listDifferenceActions", "payment:difference:handle",
                        "handleDifference", "payment:difference:handle")),
                Map.entry(PaymentSettlementSummaryController.class, Map.of(
                        "generateSettlementSummary", "payment:settlement-summary:generate",
                        "confirmSettlementSummary", "payment:settlement-summary:confirm",
                        "voidSettlementSummary", "payment:settlement-summary:void")));

        for (Map.Entry<Class<?>, Map<String, String>> controllerEntry : expectedPermissions.entrySet()) {
            for (Map.Entry<String, String> methodEntry : controllerEntry.getValue().entrySet()) {
                Method method = Arrays.stream(controllerEntry.getKey().getDeclaredMethods())
                        .filter(candidate -> candidate.getName().equals(methodEntry.getKey()))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchMethodException(methodEntry.getKey()));
                assertThat(method.getAnnotation(ApiAccess.class).permission())
                        .as(controllerEntry.getKey().getSimpleName() + "#" + methodEntry.getKey())
                        .isEqualTo(methodEntry.getValue());
            }
        }
    }

    @Test
    @DisplayName("payment task and status sync endpoints should keep original route paths")
    void paymentTaskAndSyncEndpoints_keepOriginalRoutePaths() throws NoSuchMethodException {
        Method expireMethod = PaymentTaskController.class.getDeclaredMethod("expireOpenPaymentOrders", long.class);
        Method queryMethod = PaymentTaskController.class.getDeclaredMethod("queryProcessingPaymentOrders", long.class);
        Method syncMethod = PaymentOrderController.class.getDeclaredMethod("syncPaymentOrderStatus", String.class);

        assertThat(controllerPath(PaymentTaskController.class) + expireMethod.getAnnotation(PostMapping.class).value()[0])
                .isEqualTo("/payment/tasks/expire-open-orders");
        assertThat(controllerPath(PaymentTaskController.class) + queryMethod.getAnnotation(PostMapping.class).value()[0])
                .isEqualTo("/payment/tasks/query-processing-orders");
        assertThat(controllerPath(PaymentOrderController.class) + syncMethod.getAnnotation(PostMapping.class).value()[0])
                .isEqualTo("/payment/payment-orders/sync-status");
    }

    @Test
    @DisplayName("observability and bill fetch mode endpoints should keep independent domain permissions")
    void observabilityAndBillFetchModeEndpoints_keepDomainPermissions() throws NoSuchMethodException {
        Method observability = PaymentObservabilityController.class.getDeclaredMethod("observabilitySnapshot");
        Method billFetchModes = PaymentReconciliationController.class.getDeclaredMethod("listBillFetchModes");

        assertThat(observability.getAnnotation(GetMapping.class).value()).containsExactly("/snapshot");
        assertThat(observability.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:observability:query");
        assertThat(billFetchModes.getAnnotation(GetMapping.class).value()).containsExactly("/bill-fetch-modes");
        assertThat(billFetchModes.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:reconciliation:list");
    }

    @Test
    @DisplayName("manual operation endpoints should not expose direct success mutation paths")
    void manualOperationEndpoints_doNotExposeDirectSuccessMutationPaths() {
        List<Class<?>> controllers = List.of(
                PaymentRefundOrderController.class,
                PaymentRefundApprovalController.class,
                PaymentExceptionOrderController.class,
                PaymentNotificationRecordController.class,
                PaymentOrderController.class,
                PaymentTaskController.class,
                PaymentReconciliationController.class,
                PaymentDifferenceController.class,
                PaymentSettlementSummaryController.class,
                PaymentOfflineCollectionController.class);

        List<String> postPaths = controllers.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(PostMapping.class))
                        .flatMap(method -> Arrays.stream(method.getAnnotation(PostMapping.class).value())
                                .map(path -> controllerPath(controller) + path)))
                .toList();

        assertThat(postPaths)
                .allSatisfy(path -> {
                    String normalized = path.toLowerCase(Locale.ROOT);
                    assertThat(normalized).doesNotContain("manual-success");
                    assertThat(normalized).doesNotContain("mark-success");
                    assertThat(normalized).doesNotContain("set-success");
                    assertThat(normalized).doesNotContain("force-success");
                    assertThat(normalized).doesNotContain("success/manual");
                });
    }

    @Test
    @DisplayName("offline collection and manual operation permissions should be initialized by authorization migrations")
    void paymentOperationPermissions_areInitializedByAuthorizationMigrations() throws Exception {
        List<String> requiredPermissions = List.of(
                "payment:offline-collection:list",
                "payment:offline-collection:query",
                "payment:offline-collection:confirm",
                "payment:offline-collection:refund",
                "payment:offline-collection:bank-statement:list",
                "payment:offline-collection:bank-statement:query",
                "payment:offline-collection:bank-statement:import",
                "payment:offline-collection:bank-statement:confirm",
                "payment:refund-order:query-channel",
                "payment:refund-approval:create",
                "payment:exception-order:handle",
                "payment:notification-record:retry",
                "payment:notification-record:deliver-due",
                "payment:task:expire-open-orders",
                "payment:task:query-processing-orders",
                "payment:reconciliation:import",
                "payment:difference:handle",
                "payment:settlement-summary:generate",
                "payment:settlement-summary:confirm",
                "payment:settlement-summary:void",
                "payment:observability:query");

        String migrationSql = Files.walk(findMangoRoot().resolve(
                        "mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization"))
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .map(path -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to read " + path, ex);
                    }
                })
                .reduce("", (left, right) -> left + "\n" + right);

        assertThat(migrationSql)
                .contains("线下收款")
                .contains("@/views/payment/offline-collections/index.vue")
                .contains(requiredPermissions.toArray(String[]::new));
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static String controllerPath(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertThat(mapping).as(controller.getSimpleName() + " request mapping").isNotNull();
        assertThat(mapping.value()).as(controller.getSimpleName() + " request mapping values").hasSize(1);
        return mapping.value()[0];
    }

    private static boolean isHttpEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(RequestMapping.class);
    }

    private static Path findMangoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("mango-platform"))
                    && Files.isDirectory(current.resolve("mango-platform/mango-authorization"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Mango repository root not found");
    }
}
