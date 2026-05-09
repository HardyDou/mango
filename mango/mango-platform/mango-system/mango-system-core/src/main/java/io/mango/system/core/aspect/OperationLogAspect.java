package io.mango.system.core.aspect;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.log.annotation.Log;
import io.mango.infra.web.util.JacksonUtils;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.core.service.ISysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_TEXT_LENGTH = 4000;

    private final ISysLogService logService;
    private final IpLocationResolver ipLocationResolver;

    @Around("@annotation(io.mango.infra.log.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            saveOperationLog(point, result, error, System.currentTimeMillis() - start);
        }
    }

    private void saveOperationLog(ProceedingJoinPoint point, Object result, Throwable error, long duration) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            MethodSignature signature = (MethodSignature) point.getSignature();
            Log logAnnotation = signature.getMethod().getAnnotation(Log.class);

            SysOperationLogPo opLog = new SysOperationLogPo();
            opLog.setTenantId(parseLong(MangoContextHolder.tenantId()));
            opLog.setUserId(MangoContextHolder.userId());
            opLog.setUsername(MangoContextHolder.principalName());
            opLog.setModule(logAnnotation.value());
            opLog.setOperation(logAnnotation.value());
            opLog.setMethod(request.getMethod());
            opLog.setHandlerMethod(signature.getDeclaringTypeName() + "." + signature.getName());
            opLog.setUrl(request.getRequestURI());
            opLog.setParams(truncate(resolveParams(request, point)));
            opLog.setResult(error == null ? truncate(JacksonUtils.toJsonStr(result)) : null);
            opLog.setStatus(error == null ? 1 : 0);
            opLog.setErrorMsg(error == null ? null : truncate(error.getMessage()));
            opLog.setDuration(duration);
            String clientIp = getClientIp(request);
            opLog.setIp(clientIp);
            opLog.setLocation(resolveLocation(clientIp));
            opLog.setOperateTime(LocalDateTime.now());
            logService.recordOperationLog(opLog);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }
    }

    private String resolveLocation(String clientIp) {
        if (ipLocationResolver == null) {
            return "未知";
        }
        IpLocation location = ipLocationResolver.resolve(clientIp);
        return location == null ? "未知" : location.displayText();
    }

    private String resolveParams(HttpServletRequest request, ProceedingJoinPoint point) {
        String queryParams = JacksonUtils.toJsonStr(request.getParameterMap());
        if (queryParams != null && !"{}".equals(queryParams)) {
            return queryParams;
        }
        return JacksonUtils.toJsonStr(point.getArgs());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = firstText(request.getHeader("X-Forwarded-For"), request.getHeader("X-Real-IP"));
        if (ip != null && ip.contains(",")) {
            return ip.substring(0, ip.indexOf(',')).trim();
        }
        return firstText(ip, request.getRemoteAddr());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second != null && !second.isBlank() ? second.trim() : null;
    }
}
