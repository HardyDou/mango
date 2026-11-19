package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IpWhitelistDiagnosticsTest {

    private final IpWhitelistDiagnostics diagnostics = new IpWhitelistDiagnostics();

    @Test
    void acceptsFullIpv4AndIpv6Cidrs() {
        assertThat(diagnostics.isSuspicious("10.0.0.0/8")).isFalse();
        assertThat(diagnostics.isSuspicious("172.16.0.0/12")).isFalse();
        assertThat(diagnostics.isSuspicious("192.168.0.0/16")).isFalse();
        assertThat(diagnostics.isSuspicious("127.0.0.1/32")).isFalse();
        assertThat(diagnostics.isSuspicious("::1/128")).isFalse();
        assertThat(diagnostics.isSuspicious("10.2.3.4")).isFalse();
    }

    @Test
    void rejectsShorthandNetworksThatResolveToAnotherSubnet() {
        assertThat(diagnostics.isSuspicious("10/8")).isTrue();
        assertThat(diagnostics.isSuspicious("172.16/12")).isTrue();
        assertThat(diagnostics.isSuspicious("192.168/16")).isTrue();
    }

    @Test
    void rejectsMalformedMasksAndAddresses() {
        assertThat(diagnostics.isSuspicious("10.0.0.0/33")).isTrue();
        assertThat(diagnostics.isSuspicious("::1/129")).isTrue();
        assertThat(diagnostics.isSuspicious("10.0.0.0/abc")).isTrue();
        assertThat(diagnostics.isSuspicious("not-an-address/8")).isTrue();
    }

    @Test
    void inspectsRulesWithoutFailing() {
        AccessProperties.Rule rule = new AccessProperties.Rule();
        rule.setPathPattern("/actuator/health/**");
        rule.setMethods(List.of("GET"));
        rule.setCidrs(List.of("127.0.0.1/32", "10/8"));
        AccessProperties.IpWhitelist whitelist = new AccessProperties.IpWhitelist();
        whitelist.setEnabled(true);
        whitelist.setRules(List.of(rule));

        diagnostics.inspect(whitelist);
        diagnostics.inspect(null);
    }
}
