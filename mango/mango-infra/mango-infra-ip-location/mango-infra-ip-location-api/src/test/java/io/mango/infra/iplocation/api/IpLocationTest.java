package io.mango.infra.iplocation.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class IpLocationTest {

    @Test
    void shouldExposeStableUnknownResult() {
        IpLocation location = IpLocation.empty(" 127.0.0.1 ");

        assertEquals(" 127.0.0.1 ", location.getIp());
        assertFalse(location.isResolved());
        assertEquals("未知", location.displayText());
    }

    @Test
    void shouldFormatMeaningfulFieldsOnly() {
        IpLocation location = IpLocation.empty("8.8.8.8");
        location.setCountry(" 美国 ");
        location.setProvince("0");
        location.setCity(" ");
        location.setIsp("Google");

        assertEquals("美国 Google", location.displayText());
    }

    @Test
    void shouldCopyEveryFieldIntoIndependentSnapshot() {
        IpLocation original = IpLocation.empty("8.8.8.8");
        original.setCountry("美国");
        original.setRegion("美国|0|0|0|Google");
        original.setProvince("加州");
        original.setCity("山景城");
        original.setIsp("Google");
        original.setSource("ip2region");
        original.setPrivateAddress(false);
        original.setResolved(true);

        IpLocation copy = IpLocation.copyOf(original);

        assertNotSame(original, copy);
        assertEquals(original.getIp(), copy.getIp());
        assertEquals(original.getCountry(), copy.getCountry());
        assertEquals(original.getRegion(), copy.getRegion());
        assertEquals(original.getProvince(), copy.getProvince());
        assertEquals(original.getCity(), copy.getCity());
        assertEquals(original.getIsp(), copy.getIsp());
        assertEquals(original.getSource(), copy.getSource());
        assertEquals(original.isPrivateAddress(), copy.isPrivateAddress());
        assertEquals(original.isResolved(), copy.isResolved());
    }
}
