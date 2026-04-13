package io.mango.system.core.aspect;

import io.mango.common.annotation.Log;
import io.mango.common.util.JacksonUtils;
import io.mango.system.core.entity.SysOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    @Around("@annotation(io.mango.common.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = point.proceed();
        long duration = System.currentTimeMillis() - start;

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            MethodSignature signature = (MethodSignature) point.getSignature();
            Log logAnnotation = signature.getMethod().getAnnotation(Log.class);

            SysOperationLog opLog = new SysOperationLog();
            opLog.setModule(logAnnotation.value());
            opLog.setOperation(logAnnotation.value());
            opLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
            opLog.setUrl(request.getRequestURI());
            opLog.setParams(JacksonUtils.toJsonStr(request.getParameterMap()));
            opLog.setDuration(duration);
            opLog.setIp(getClientIp(request));
            opLog.setStatus(1);
            opLog.setOperateTime(java.time.LocalDateTime.now());

            log.info("Operation log: {}", JacksonUtils.toJsonStr(opLog));
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
