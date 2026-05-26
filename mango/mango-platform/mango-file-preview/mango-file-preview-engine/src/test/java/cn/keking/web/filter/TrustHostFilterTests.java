package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrustHostFilterTests {

    private final TrustHostFilter trustHostFilter = new TrustHostFilter();

    @AfterEach
    void tearDown() {
        ConfigConstants.setTrustHostValue("default");
        ConfigConstants.setNotTrustHostValue("default");
    }

    @Test
    void shouldBlockWildcardNotTrustHostPattern() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("192.168.*");

        assertTrue(trustHostFilter.isNotTrustHost("192.168.1.10"));
        assertFalse(trustHostFilter.isNotTrustHost("8.8.8.8"));
        assertFalse(trustHostFilter.isNotTrustHost("192.168.evil.com"));
    }

    @Test
    void shouldBlockCidrNotTrustHostPattern() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("10.0.0.0/8");

        assertTrue(trustHostFilter.isNotTrustHost("10.1.2.3"));
        assertFalse(trustHostFilter.isNotTrustHost("11.1.2.3"));
        // Ensure hostnames are not matched by CIDR-based not-trust rules (no DNS resolution)
        assertFalse(trustHostFilter.isNotTrustHost("localhost"));
    }

    @Test
    void shouldSupportHighBitIpv4InCidrMatching() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("200.0.0.0/8");

        assertTrue(trustHostFilter.isNotTrustHost("200.1.2.3"));
        assertFalse(trustHostFilter.isNotTrustHost("199.1.2.3"));
    }

    @Test
    void shouldSupportIpv4UpperBoundaryCidrMatching() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("255.255.255.255/32");

        assertTrue(trustHostFilter.isNotTrustHost("255.255.255.255"));
        assertFalse(trustHostFilter.isNotTrustHost("255.255.255.254"));
    }

    @Test
    void shouldDenyWhenHostIsBlankOrNull() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("default");

        assertTrue(trustHostFilter.isNotTrustHost(null));
        assertTrue(trustHostFilter.isNotTrustHost(" "));
    }

    @Test
    void shouldAllowWildcardTrustHostPattern() {
        ConfigConstants.setTrustHostValue("*.trusted.com");
        ConfigConstants.setNotTrustHostValue("default");

        assertFalse(trustHostFilter.isNotTrustHost("api.trusted.com"));
        assertTrue(trustHostFilter.isNotTrustHost("api.evil.com"));
    }

    @Test
    void shouldKeepBlacklistHigherPriorityThanWhitelist() {
        ConfigConstants.setTrustHostValue("*");
        ConfigConstants.setNotTrustHostValue("127.0.0.1,10.*");

        assertTrue(trustHostFilter.isNotTrustHost("127.0.0.1"));
        assertTrue(trustHostFilter.isNotTrustHost("10.1.2.3"));
        assertFalse(trustHostFilter.isNotTrustHost("8.8.8.8"));
    }

    @Test
    void shouldStillEnforceWhitelistWhenBlacklistConfigured() {
        ConfigConstants.setTrustHostValue("internal.example.com");
        ConfigConstants.setNotTrustHostValue("127.0.0.1");

        assertFalse(trustHostFilter.isNotTrustHost("internal.example.com"));
        assertTrue(trustHostFilter.isNotTrustHost("8.8.8.8"));
    }

    @Test
    void shouldAllowInternalCompressEntryUrl() {
        String url = "http://127.0.0.1:5173/api/mango-preview-e2e.zip_/mango-preview-zip-entry.txt"
                + "?kkCompressfileKey=mango-preview-e2e.zip_"
                + "&kkCompressfilepath=mango-preview-e2e.zip_%2Fmango-preview-zip-entry.txt"
                + "&fullfilename=mango-preview-zip-entry.txt";

        assertTrue(trustHostFilter.isInternalCompressEntryUrl(url));
    }

    @Test
    void shouldAllowInternalCompressedFileEndpointUrl() {
        String url = "http://127.0.0.1:5173/api/compressed-file"
                + "?kkCompressfileKey=mango-preview-e2e.zip_"
                + "&kkCompressfilepath=mango-preview-e2e.zip_%2Fsample.pdf"
                + "&fullfilename=sample.pdf";

        assertTrue(trustHostFilter.isInternalCompressEntryUrl(url));
    }

    @Test
    void shouldAllowInternalCompressedFileEndpointUrlWithEncodedChineseZipName() {
        String url = "http://127.0.0.1:5581/api/compressed-file"
                + "?kkCompressfileKey=%E5%BD%92%E6%A1%A3.zip_"
                + "&kkCompressfilepath=%E5%BD%92%E6%A1%A3.zip_%2FwKgoC2C5dB6AYzBLAA0nUxSwXjw727.pdf"
                + "&fullfilename=wKgoC2C5dB6AYzBLAA0nUxSwXjw727.pdf";

        assertTrue(trustHostFilter.isInternalCompressEntryUrl(url));
    }

    @Test
    void shouldRejectPlainLocalhostUrlAsInternalCompressEntryUrl() {
        String url = "http://127.0.0.1:5173/api/file-preview/files/download/1";

        assertFalse(trustHostFilter.isInternalCompressEntryUrl(url));
    }
}
