package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReadonlyResourceControllerTest {

    @Test
    @DisplayName("transaction flow endpoints should be read only and have no delete mapping")
    void transactionFlowEndpoints_areReadOnly() {
        List<Method> transactionFlowMethods = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                .filter(method -> hasPath(method, "/transaction-flows"))
                .toList();

        assertThat(transactionFlowMethods).hasSize(2);
        assertThat(transactionFlowMethods)
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("pageTransactionFlows", "detailTransactionFlow");
        assertThat(transactionFlowMethods)
                .allMatch(method -> method.isAnnotationPresent(GetMapping.class))
                .noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class));
    }

    @Test
    @DisplayName("transaction flow endpoints should expose only list and query permissions")
    void transactionFlowEndpoints_useQueryPermissionsOnly() {
        List<String> permissions = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                .filter(method -> hasPath(method, "/transaction-flows"))
                .map(method -> method.getAnnotation(ApiAccess.class))
                .map(ApiAccess::permission)
                .toList();

        assertThat(permissions)
                .containsExactlyInAnyOrder("payment:transaction-flow:list", "payment:transaction-flow:query");
        assertThat(permissions)
                .noneMatch(permission -> permission.contains("delete") || permission.contains("remove"));
    }

    @Test
    @DisplayName("offline collection endpoints should include channel owned collection operations with dedicated permissions")
    void offlineCollectionEndpoints_includeChannelOwnedOperationsWithDedicatedPermissions() {
        List<Method> methods = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                .filter(method -> hasPath(method, "/offline-collections"))
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
        assertThat(methods)
                .noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class));
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
    @DisplayName("offline collection menu and permissions should be initialized by authorization migrations")
    void offlineCollectionPermissions_areInitializedByAuthorizationMigrations() throws Exception {
        Path migrationDir = findMangoRoot().resolve(
                "mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization");
        String migrationSql = Files.walk(migrationDir)
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
                .contains(
                        "payment:offline-collection:list",
                        "payment:offline-collection:query",
                        "payment:offline-collection:confirm",
                        "payment:offline-collection:refund",
                        "payment:offline-collection:bank-statement:list",
                        "payment:offline-collection:bank-statement:query",
                        "payment:offline-collection:bank-statement:import",
                        "payment:offline-collection:bank-statement:confirm");
    }

    @Test
    @DisplayName("notification dispatch endpoint should use independent deliver-due permission")
    void notificationDeliverDueEndpoint_usesIndependentPermission() throws NoSuchMethodException {
        Method method = PaymentReadonlyResourceController.class.getDeclaredMethod("deliverDueNotificationRecords", long.class);

        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/notification-records/deliver-due");
        assertThat(method.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:notification-record:deliver-due");
    }

    @Test
    @DisplayName("payment task endpoints should use independent task permissions")
    void paymentTaskEndpoints_useIndependentPermissions() throws NoSuchMethodException {
        Method expireMethod = PaymentReadonlyResourceController.class.getDeclaredMethod("expireOpenPaymentOrders", long.class);
        Method queryMethod = PaymentReadonlyResourceController.class.getDeclaredMethod("queryProcessingPaymentOrders", long.class);

        assertThat(expireMethod.getAnnotation(PostMapping.class).value()).containsExactly("/tasks/expire-open-orders");
        assertThat(expireMethod.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:task:expire-open-orders");
        assertThat(queryMethod.getAnnotation(PostMapping.class).value()).containsExactly("/tasks/query-processing-orders");
        assertThat(queryMethod.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:task:query-processing-orders");
    }

    @Test
    @DisplayName("refund approval endpoints should use independent permissions")
    void refundApprovalEndpoints_useIndependentPermissions() {
        List<String> permissions = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                .filter(method -> hasPath(method, "/refund-approvals"))
                .map(method -> method.getAnnotation(ApiAccess.class))
                .map(ApiAccess::permission)
                .toList();

        assertThat(permissions)
                .containsExactlyInAnyOrder(
                        "payment:refund-approval:list",
                        "payment:refund-approval:query",
                        "payment:refund-approval:list",
                        "payment:refund-approval:create");
    }

    @Test
    @DisplayName("PAY-OPS-001 manual operation endpoints should use dedicated permissions")
    void payOps001ManualOperationEndpoints_useDedicatedPermissions() throws NoSuchMethodException {
        Map<String, String> expectedPermissions = Map.ofEntries(
                Map.entry("queryRefundOrder", "payment:refund-order:query-channel"),
                Map.entry("createRefundApproval", "payment:refund-approval:create"),
                Map.entry("listExceptionOrderActions", "payment:exception-order:handle"),
                Map.entry("handleExceptionOrder", "payment:exception-order:handle"),
                Map.entry("retryNotificationRecord", "payment:notification-record:retry"),
                Map.entry("deliverDueNotificationRecords", "payment:notification-record:deliver-due"),
                Map.entry("expireOpenPaymentOrders", "payment:task:expire-open-orders"),
                Map.entry("queryProcessingPaymentOrders", "payment:task:query-processing-orders"),
                Map.entry("importReconciliation", "payment:reconciliation:import"),
                Map.entry("generateMangoPayVirtualBill", "payment:reconciliation:import"),
                Map.entry("listDifferenceActions", "payment:difference:handle"),
                Map.entry("handleDifference", "payment:difference:handle"),
                Map.entry("generateSettlementSummary", "payment:settlement-summary:generate"),
                Map.entry("confirmSettlementSummary", "payment:settlement-summary:confirm"),
                Map.entry("voidSettlementSummary", "payment:settlement-summary:void"));

        for (Map.Entry<String, String> entry : expectedPermissions.entrySet()) {
            Method method = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException(entry.getKey()));
            assertThat(method.getAnnotation(ApiAccess.class).permission())
                    .as(entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("PAY-OPS-001 manual operation permissions should be initialized by authorization migrations")
    void payOps001ManualOperationPermissions_areInitializedByAuthorizationMigrations() throws Exception {
        List<String> requiredPermissions = List.of(
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
                "payment:settlement-summary:void");
        Path migrationDir = findMangoRoot().resolve(
                "mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization");
        String migrationSql = Files.walk(migrationDir)
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

        assertThat(migrationSql).contains(requiredPermissions.toArray(String[]::new));
    }

    @Test
    @DisplayName("observability endpoint should be read only and use independent query permission")
    void observabilityEndpoint_usesIndependentQueryPermission() throws NoSuchMethodException {
        Method method = PaymentReadonlyResourceController.class.getDeclaredMethod("observabilitySnapshot");

        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/observability/snapshot");
        assertThat(method.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:observability:query");
    }

    @Test
    @DisplayName("observability permission should be initialized by authorization migrations")
    void observabilityPermission_isInitializedByAuthorizationMigrations() throws Exception {
        Path migrationDir = findMangoRoot().resolve(
                "mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization");
        String migrationSql = Files.walk(migrationDir)
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

        assertThat(migrationSql).contains("payment:observability:query");
    }

    @Test
    @DisplayName("bill fetch mode endpoint should be read only and use reconciliation list permission")
    void billFetchModeEndpoint_usesReadonlyPermission() throws NoSuchMethodException {
        Method method = PaymentReadonlyResourceController.class.getDeclaredMethod("listBillFetchModes");

        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/reconciliations/bill-fetch-modes");
        assertThat(method.getAnnotation(ApiAccess.class).permission()).isEqualTo("payment:reconciliation:list");
    }

    @Test
    @DisplayName("manual operation endpoints should not expose direct success mutation paths")
    void manualOperationEndpoints_doNotExposeDirectSuccessMutationPaths() {
        List<String> postPaths = Arrays.stream(PaymentReadonlyResourceController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .flatMap(method -> Arrays.stream(method.getAnnotation(PostMapping.class).value()))
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

    private static boolean hasPath(Method method, String pathPrefix) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        return containsPath(getMapping == null ? null : getMapping.value(), pathPrefix)
                || containsPath(postMapping == null ? null : postMapping.value(), pathPrefix)
                || containsPath(deleteMapping == null ? null : deleteMapping.value(), pathPrefix)
                || containsPath(requestMapping == null ? null : requestMapping.value(), pathPrefix);
    }

    private static boolean containsPath(String[] paths, String pathPrefix) {
        return paths != null && Arrays.stream(paths).anyMatch(path -> path.startsWith(pathPrefix));
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
