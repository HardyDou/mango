package io.mango.auth.starter.config;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.vo.SecurityPrincipalVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.function.Supplier;

/** Dedicated fail-closed authorization for module runtime diagnostics. */
public class ModuleDiagnosticAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleDiagnosticAuthorizationManager.class);
    private static final String PLATFORM_TENANT = "1";
    public static final String BEAN_NAME = "mangoModuleDiagnosticAuthorizationManager";
    public static final String REQUIRED_PERMISSION = "diagnostic:read";

    private final IAuthorizationProvider authorizationProvider;

    public ModuleDiagnosticAuthorizationManager(IAuthorizationProvider authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        try {
            HttpServletRequest request = context.getRequest();
            String[] requestedApps = request.getParameterValues("app");
            String requestedApp = requestedApps != null && requestedApps.length == 1
                    ? requestedApps[0]
                    : null;
            if (!isLoopback(request.getRemoteAddr())) {
                return deny(requestedApp, null, request.getRemoteAddr(), "REMOTE_NOT_LOOPBACK");
            }
            if (!hasHeaderOnlyBearer(request)) {
                return deny(requestedApp, null, request.getRemoteAddr(), "HEADER_BEARER_REQUIRED");
            }
            Authentication authentication = authenticationSupplier.get();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken
                    || !(authentication.getPrincipal() instanceof SecurityPrincipalVO principal)
                    || principal.memberId() == null
                    || isBlank(principal.tenantId())
                    || isBlank(principal.appCode())) {
                return deny(requestedApp, null, request.getRemoteAddr(), "PRINCIPAL_SCOPE_INVALID");
            }
            if (isBlank(requestedApp)
                    || !principal.appCode().equals(requestedApp)
                    || !PLATFORM_TENANT.equals(principal.tenantId())) {
                return deny(requestedApp, principal.memberId(), request.getRemoteAddr(), "REQUEST_SCOPE_MISMATCH");
            }
            AuthorizationQuery query = AuthorizationQuery.member(principal.memberId())
                    .withTenantId(principal.tenantId())
                    .withSystemCode(principal.appCode())
                    .withRealm(principal.realm())
                    .withActorType(principal.actorType())
                    .withParty(principal.partyType(), principal.partyId());
            AuthorizationSnapshotVO snapshot = authorizationProvider.load(query);
            boolean granted = snapshot != null && snapshot.permissionCodes().stream()
                    .anyMatch(permission -> REQUIRED_PERMISSION.equals(permission) || "*:*".equals(permission));
            LOG.info(
                    "module_diagnostic_authorization app={} memberId={} remote={} decision={} reason={}",
                    requestedApp,
                    principal.memberId(),
                    request.getRemoteAddr(),
                    granted ? "ALLOW" : "DENY",
                    granted ? "PERMISSION_GRANTED" : "PERMISSION_MISSING");
            return new AuthorizationDecision(granted);
        } catch (RuntimeException exception) {
            LOG.warn("module_diagnostic_authorization decision=DENY reason=AUTHORIZATION_ERROR errorType={}",
                    exception.getClass().getSimpleName());
            return new AuthorizationDecision(false);
        }
    }

    private AuthorizationDecision deny(String app, Long memberId, String remoteAddress, String reason) {
        LOG.info(
                "module_diagnostic_authorization app={} memberId={} remote={} decision=DENY reason={}",
                safeAuditValue(app),
                memberId == null ? "unknown" : memberId,
                safeAuditValue(remoteAddress),
                reason);
        return new AuthorizationDecision(false);
    }

    private boolean isLoopback(String remoteAddress) {
        return "127.0.0.1".equals(remoteAddress)
                || "::1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress);
    }

    private boolean hasHeaderOnlyBearer(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) || request.getParameterMap().containsKey("token")) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("MANGO_TOKEN".equals(cookie.getName())) {
                    return false;
                }
            }
        }
        Enumeration<String> headers = request.getHeaders("Authorization");
        if (headers == null || !headers.hasMoreElements()) {
            return false;
        }
        String header = headers.nextElement();
        if (headers.hasMoreElements() || header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        String token = header.substring("Bearer ".length());
        return !token.isBlank() && token.chars().noneMatch(Character::isWhitespace);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeAuditValue(String value) {
        if (isBlank(value) || value.length() > 80 || !value.matches("[A-Za-z0-9:.\\-]+")) {
            return "unknown";
        }
        return value;
    }
}
