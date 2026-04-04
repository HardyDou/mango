package io.mango.common.permission;

import io.mango.common.annotation.Perm;
import io.mango.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Permission aspect for @Perm annotation enforcement.
 * <p>
 * Intercepts methods annotated with @Perm and checks if the current user
 * has the required permission before allowing method execution.
 *
 * @author Mango
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermAspect {

    private static final String USER_ID_ATTRIBUTE = "userId";

    /**
     * Permission service - injected via setter for flexibility
     */
    private static IPermissionService permissionService;

    /**
     * Set the permission service (called by Spring or manually)
     */
    public void setPermissionService(IPermissionService service) {
        permissionService = service;
    }

    @Before("@annotation(io.mango.common.annotation.Perm)")
    public void checkPermission(JoinPoint joinPoint) {
        if (permissionService == null) {
            log.warn("PermissionService not configured, skipping permission check");
            return;
        }

        // Get the @Perm annotation from the method
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Perm perm = signature.getMethod().getAnnotation(Perm.class);
        String requiredPerm = perm.value();

        // Get current user ID from request attribute
        Long userId = getCurrentUserId();
        if (userId == null) {
            log.debug("No user ID found in request context, permission check skipped");
            return;
        }

        // Get user's permissions
        List<String> userPerms = permissionService.listUserPermissions(userId);

        // Check if user has the required permission
        if (!userPerms.contains(requiredPerm)) {
            log.warn("User {} attempted to access resource requiring permission {}", userId, requiredPerm);
            throw new BizException(403, "没有访问该资源的权限: " + perm.desc());
        }

        log.debug("User {} authorized for permission {}", userId, requiredPerm);
    }

    /**
     * Get current user ID from request attributes
     */
    private Long getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
                if (userId instanceof Long) {
                    return (Long) userId;
                }
                if (userId instanceof Integer) {
                    return ((Integer) userId).longValue();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get user ID from request context", e);
        }
        return null;
    }
}
