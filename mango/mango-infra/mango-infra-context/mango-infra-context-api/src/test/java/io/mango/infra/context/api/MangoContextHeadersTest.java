package io.mango.infra.context.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MangoContextHeadersTest {

    @Test
    void all_containsEveryHeaderExactlyOnceInContractOrder() {
        assertEquals(List.of(
                "X-Mango-Request-Id", "X-Mango-Trace-Id", "X-Mango-Tenant-Id",
                "X-Mango-User-Id", "X-Mango-Member-Id", "X-Mango-Principal-Name",
                "X-Mango-Realm", "X-Mango-Actor-Type", "X-Mango-Party-Type",
                "X-Mango-Party-Id", "X-Mango-App-Code", "X-Mango-Client-Ip"
        ), MangoContextHeaders.ALL);
        assertEquals(MangoContextHeaders.ALL.size(), MangoContextHeaders.ALL.stream().distinct().count());
    }

    @Test
    void all_isImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> MangoContextHeaders.ALL.add("X-Mango-Unknown"));
    }
}
