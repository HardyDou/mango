package io.mango.infra.context.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoContextHolderTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void get_withoutContextReturnsEmptySnapshot() {
        assertTrue(MangoContextHolder.get().isEmpty());
        assertNull(MangoContextHolder.token());
    }

    @Test
    void set_exposesSnapshotAndConvenienceAccessors() {
        MangoContextSnapshot snapshot = new MangoContextSnapshot(
                "request-1", "trace-1", "tenant-1", 11L, 12L, "principal",
                "realm", "actor", "party", 13L, "admin", "10.0.0.1");

        MangoContextHolder.set(snapshot);

        assertSame(snapshot, MangoContextHolder.get());
        assertEquals("request-1", MangoContextHolder.requestId());
        assertEquals("trace-1", MangoContextHolder.traceId());
        assertEquals("tenant-1", MangoContextHolder.tenantId());
        assertEquals(11L, MangoContextHolder.userId());
        assertEquals(12L, MangoContextHolder.memberId());
        assertEquals("principal", MangoContextHolder.principalName());
        assertEquals("admin", MangoContextHolder.appCode());
        assertEquals("10.0.0.1", MangoContextHolder.clientIp());
    }

    @Test
    void update_appliesTransformationAndNullUpdaterIsNoOp() {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", null, "tenant-1", null, null));

        MangoContextHolder.update(snapshot -> snapshot.withTenantId("tenant-2"));
        MangoContextHolder.update(null);

        assertEquals("tenant-2", MangoContextHolder.tenantId());
    }

    @Test
    void token_blankValueClearsOnlyToken() {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", null, null, null, null));
        MangoContextHolder.setToken(" token-1 ");

        assertEquals(" token-1 ", MangoContextHolder.token());
        MangoContextHolder.setToken(" ");

        assertNull(MangoContextHolder.token());
        assertEquals("request-1", MangoContextHolder.requestId());
    }

    @Test
    void clearToken_preservesSnapshot() {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", null, null, null, null));
        MangoContextHolder.setToken("token-1");

        MangoContextHolder.clearToken();

        assertNull(MangoContextHolder.token());
        assertEquals("request-1", MangoContextHolder.requestId());
    }

    @Test
    void settingNullOrEmptySnapshotClearsSnapshotAndToken() {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", null, null, null, null));
        MangoContextHolder.setToken("token-1");

        MangoContextHolder.set(MangoContextSnapshot.empty());

        assertTrue(MangoContextHolder.get().isEmpty());
        assertNull(MangoContextHolder.token());

        MangoContextHolder.set(MangoContextSnapshot.request("request-2", null, null, null, null));
        MangoContextHolder.setToken("token-2");
        MangoContextHolder.set(null);

        assertTrue(MangoContextHolder.get().isEmpty());
        assertNull(MangoContextHolder.token());
    }
}
