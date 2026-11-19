package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpWhitelistMatcherTest {

    private final IpWhitelistMatcher matcher = new IpWhitelistMatcher();

    @Test
    void matches_shouldSupportIpv4Ipv6MethodsAndAntPaths() {
        AccessProperties.IpWhitelist whitelist = whitelist("/actuator/**", List.of("GET"),
                List.of("10.0.0.0/8", "::1/128"));
        assertTrue(matcher.matches(whitelist, "GET", "/actuator/health", "10.2.3.4"));
        assertTrue(matcher.matches(whitelist, "get", "/actuator/info", "0:0:0:0:0:0:0:1"));
        assertFalse(matcher.matches(whitelist, "POST", "/actuator/health", "10.2.3.4"));
        assertFalse(matcher.matches(whitelist, "GET", "/users", "10.2.3.4"));
    }

    @Test
    void matches_shouldRejectInvalidOrCrossFamilyCidrs() {
        AccessProperties.IpWhitelist whitelist = whitelist("/**", List.of("ALL"),
                List.of("10.0.0.0/99", "bad-cidr", "::1/128"));
        assertFalse(matcher.matches(whitelist, "GET", "/any", "10.2.3.4"));
        assertFalse(matcher.matches(whitelist, "GET", "/any", null));
    }

    @Test
    void matches_shouldTreatIpv4MappedAddressAsIpv4() {
        AccessProperties.IpWhitelist whitelist = whitelist("/actuator/health/**", List.of("GET"),
                List.of("127.0.0.1/32"));
        assertTrue(matcher.matches(whitelist, "GET", "/actuator/health/readiness", "::ffff:127.0.0.1"));
        assertTrue(matcher.matches(whitelist, "GET", "/actuator/health", "127.0.0.1"));
    }

    @Test
    void matches_shouldMatchExactAddressWithoutCidrSuffix() {
        AccessProperties.IpWhitelist whitelist = whitelist("/actuator/health/**", List.of("GET"),
                List.of("10.2.3.4"));
        assertTrue(matcher.matches(whitelist, "GET", "/actuator/health/liveness", "10.2.3.4"));
        assertFalse(matcher.matches(whitelist, "GET", "/actuator/health/liveness", "10.2.3.5"));
    }

    @Test
    void matches_shouldNotLetShorthandNetworkWidenTheRule() {
        AccessProperties.IpWhitelist whitelist = whitelist("/actuator/health/**", List.of("GET"),
                List.of("10/8"));
        assertFalse(matcher.matches(whitelist, "GET", "/actuator/health/readiness", "10.2.3.4"));
    }

    @Test
    void matches_shouldNotFallBackToPlatformRulesWhenTheListIsReplaced() {
        AccessProperties.IpWhitelist whitelist = whitelist("/actuator/health", List.of("GET"),
                List.of("127.0.0.1/32", "::1/128"));
        assertTrue(matcher.matches(whitelist, "GET", "/actuator/health", "127.0.0.1"));
        assertFalse(matcher.matches(whitelist, "GET", "/actuator/health/readiness", "127.0.0.1"));
        assertFalse(matcher.matches(whitelist, "GET", "/actuator/health/liveness", "::1"));
        assertFalse(matcher.matches(whitelist, "GET", "/actuator/info", "127.0.0.1"));
    }

    private AccessProperties.IpWhitelist whitelist(String path, List<String> methods, List<String> cidrs) {
        AccessProperties.Rule rule = new AccessProperties.Rule();
        rule.setPathPattern(path);
        rule.setMethods(methods);
        rule.setCidrs(cidrs);
        AccessProperties.IpWhitelist whitelist = new AccessProperties.IpWhitelist();
        whitelist.setEnabled(true);
        whitelist.setRules(List.of(rule));
        return whitelist;
    }
}
