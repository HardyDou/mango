package io.mango.infra.context.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoContextSnapshotTest {

    @Test
    void empty_hasNoValues() {
        MangoContextSnapshot snapshot = MangoContextSnapshot.empty();

        assertTrue(snapshot.isEmpty());
        assertNull(snapshot.requestId());
        assertNull(snapshot.partyId());
    }

    @Test
    void constructor_trimsTextAndNormalizesBlankValues() {
        MangoContextSnapshot snapshot = new MangoContextSnapshot(
                " request-1 ", " ", " tenant-1 ", 1L, 2L, " principal ",
                " realm ", " actor ", " party ", 3L, " app ", " 10.0.0.1 ");

        assertEquals("request-1", snapshot.requestId());
        assertNull(snapshot.traceId());
        assertEquals("tenant-1", snapshot.tenantId());
        assertEquals("principal", snapshot.principalName());
        assertEquals("realm", snapshot.realm());
        assertEquals("actor", snapshot.actorType());
        assertEquals("party", snapshot.partyType());
        assertEquals("app", snapshot.appCode());
        assertEquals("10.0.0.1", snapshot.clientIp());
        assertFalse(snapshot.isEmpty());
    }

    @Test
    void request_populatesOnlyRequestFields() {
        MangoContextSnapshot snapshot = MangoContextSnapshot.request(
                "request-1", "trace-1", "tenant-1", "admin", "10.0.0.1");

        assertEquals("request-1", snapshot.requestId());
        assertEquals("trace-1", snapshot.traceId());
        assertEquals("tenant-1", snapshot.tenantId());
        assertEquals("admin", snapshot.appCode());
        assertEquals("10.0.0.1", snapshot.clientIp());
        assertNull(snapshot.userId());
    }

    @Test
    void withRequest_overridesPresentValuesAndRetainsExistingForBlankValues() {
        MangoContextSnapshot original = MangoContextSnapshot.request(
                "request-old", "trace-old", "tenant-old", "app-old", "10.0.0.1");

        MangoContextSnapshot changed = original.withRequest(
                " request-new ", " ", null, "app-new", "10.0.0.2");

        assertEquals("request-new", changed.requestId());
        assertEquals("trace-old", changed.traceId());
        assertEquals("tenant-old", changed.tenantId());
        assertEquals("app-new", changed.appCode());
        assertEquals("10.0.0.2", changed.clientIp());
        assertEquals("request-old", original.requestId());
    }

    @Test
    void withSecurity_mergesSecurityValuesWithoutChangingRequestValues() {
        MangoContextSnapshot original = MangoContextSnapshot.request(
                "request-1", "trace-1", "tenant-request", "app-request", "10.0.0.1");

        MangoContextSnapshot changed = original.withSecurity(
                11L, 12L, "tenant-security", "principal", "member", "user",
                "account", 13L, "app-security");

        assertEquals("request-1", changed.requestId());
        assertEquals("trace-1", changed.traceId());
        assertEquals("10.0.0.1", changed.clientIp());
        assertEquals("tenant-security", changed.tenantId());
        assertEquals(11L, changed.userId());
        assertEquals(12L, changed.memberId());
        assertEquals("principal", changed.principalName());
        assertEquals("member", changed.realm());
        assertEquals("user", changed.actorType());
        assertEquals("account", changed.partyType());
        assertEquals(13L, changed.partyId());
        assertEquals("app-security", changed.appCode());
    }

    @Test
    void withSecurity_overloadAndBlankValuesRetainExistingSecurity() {
        MangoContextSnapshot original = MangoContextSnapshot.empty()
                .withSecurity(11L, 12L, "tenant", "principal", "member", "user",
                        "account", 13L, "admin");

        MangoContextSnapshot changed = original.withSecurity(
                null, " ", null, "", " ", null, null, " ");

        assertEquals(11L, changed.userId());
        assertEquals(12L, changed.memberId());
        assertEquals("tenant", changed.tenantId());
        assertEquals("principal", changed.principalName());
        assertEquals("member", changed.realm());
        assertEquals("user", changed.actorType());
        assertEquals("account", changed.partyType());
        assertEquals(13L, changed.partyId());
        assertEquals("admin", changed.appCode());
    }

    @Test
    void withTenantId_usesNormalizedNewValueOrRetainsExistingValue() {
        MangoContextSnapshot original = MangoContextSnapshot.request(null, null, "tenant-old", null, null);

        assertEquals("tenant-new", original.withTenantId(" tenant-new ").tenantId());
        assertEquals("tenant-old", original.withTenantId(" ").tenantId());
        assertEquals("tenant-old", original.tenantId());
    }
}
