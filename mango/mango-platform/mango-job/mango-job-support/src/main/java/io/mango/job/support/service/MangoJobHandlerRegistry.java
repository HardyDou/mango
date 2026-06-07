package io.mango.job.support.service;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 当前应用内 Job 处理器注册表。
 */
@Service
public class MangoJobHandlerRegistry implements IMangoJobHandlerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobHandlerRegistry.class);

    private final Map<String, MangoJobHandlerVO> handlers = new LinkedHashMap<>();

    private final Map<String, MangoJobHandler> handlerBeans = new LinkedHashMap<>();

    private final MangoNativeJobProperties properties;

    private final String springApplicationName;

    public MangoJobHandlerRegistry(ObjectProvider<MangoJobHandler> provider,
                                   MangoNativeJobProperties properties,
                                   @Value("${spring.application.name:}") String springApplicationName) {
        this.properties = properties;
        this.springApplicationName = springApplicationName;
        provider.orderedStream().forEach(this::register);
    }

    @Override
    public synchronized void register(MangoJobHandler handler) {
        Require.notNull(handler, "Job 处理器不能为空");
        String handlerName = MangoJobHandlerSupport.normalizeRequired(handler.handlerName(), "Job 处理器名称不能为空");
        String appCode = MangoJobHandlerSupport.trimToNull(handler.appCode());
        if (appCode == null) {
            appCode = MangoJobHandlerSupport.trimToNull(properties.getAppCode());
        }
        if (appCode == null) {
            appCode = MangoJobHandlerSupport.trimToNull(springApplicationName);
        }
        if (appCode == null) {
            appCode = "local";
        }
        String serviceCode = MangoJobHandlerSupport.trimToNull(handler.serviceCode());
        if (serviceCode == null) {
            serviceCode = MangoJobHandlerSupport.trimToNull(properties.getServiceCode());
        }
        if (serviceCode == null) {
            serviceCode = appCode;
        }
        String workerGroup = MangoJobHandlerSupport.trimToNull(handler.workerGroup());
        if (workerGroup == null) {
            workerGroup = MangoJobHandlerSupport.trimToNull(properties.getWorkerGroup());
        }
        if (workerGroup == null) {
            workerGroup = serviceCode;
        }
        Set<String> supportedJobCodes = normalizedJobCodes(handler.supportedJobCodes());
        String key = key(appCode, serviceCode, workerGroup, handlerName);
        Require.isTrue(!handlers.containsKey(key), "Job 处理器名称重复：" + handlerName);

        MangoJobHandlerVO vo = new MangoJobHandlerVO();
        vo.setAppCode(appCode);
        vo.setServiceCode(serviceCode);
        vo.setWorkerGroup(workerGroup);
        vo.setHandlerName(handlerName);
        vo.setSupportedJobCodes(supportedJobCodes);
        vo.setJobType(JobType.BUILTIN.name());
        vo.setConcurrent(Boolean.TRUE);
        handlers.put(key, vo);
        handlerBeans.put(key, handler);
        LOGGER.info("Mango job handler registered, appCode={}, serviceCode={}, workerGroup={}, handlerName={}, supportedJobCodes={}",
                appCode, serviceCode, workerGroup, handlerName, supportedJobCodes);
    }

    @Override
    public synchronized List<MangoJobHandlerVO> listHandlers() {
        return handlers.values().stream()
                .sorted(Comparator.comparing(MangoJobHandlerVO::getAppCode)
                .thenComparing(MangoJobHandlerVO::getServiceCode)
                .thenComparing(MangoJobHandlerVO::getWorkerGroup)
                .thenComparing(MangoJobHandlerVO::getHandlerName))
                .toList();
    }

    @Override
    public synchronized Optional<MangoJobHandler> findHandler(String handlerName) {
        return findHandler(null, handlerName);
    }

    @Override
    public synchronized Optional<MangoJobHandler> findHandler(String appCode, String handlerName) {
        String normalized = MangoJobHandlerSupport.trimToNull(handlerName);
        if (normalized == null) {
            return Optional.empty();
        }
        String normalizedAppCode = MangoJobHandlerSupport.trimToNull(appCode);
        for (Map.Entry<String, MangoJobHandlerVO> entry : handlers.entrySet()) {
            MangoJobHandlerVO metadata = entry.getValue();
            if (!normalized.equals(metadata.getHandlerName())) {
                continue;
            }
            if (normalizedAppCode != null && !normalizedAppCode.equals(metadata.getAppCode())) {
                continue;
            }
            return Optional.ofNullable(handlerBeans.get(entry.getKey()));
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<MangoJobHandler> findHandler(String appCode,
                                                              String serviceCode,
                                                              String workerGroup,
                                                              String handlerName,
                                                              String jobCode) {
        String normalizedHandlerName = MangoJobHandlerSupport.trimToNull(handlerName);
        if (normalizedHandlerName == null) {
            return Optional.empty();
        }
        String normalizedAppCode = MangoJobHandlerSupport.trimToNull(appCode);
        String normalizedServiceCode = MangoJobHandlerSupport.trimToNull(serviceCode);
        String normalizedWorkerGroup = MangoJobHandlerSupport.trimToNull(workerGroup);
        String normalizedJobCode = MangoJobHandlerSupport.trimToNull(jobCode);
        for (Map.Entry<String, MangoJobHandlerVO> entry : handlers.entrySet()) {
            MangoJobHandlerVO metadata = entry.getValue();
            if (!normalizedHandlerName.equals(metadata.getHandlerName())) {
                continue;
            }
            if (normalizedAppCode != null && !normalizedAppCode.equals(metadata.getAppCode())) {
                continue;
            }
            if (normalizedServiceCode != null && !normalizedServiceCode.equals(metadata.getServiceCode())) {
                continue;
            }
            if (normalizedWorkerGroup != null && !normalizedWorkerGroup.equals(metadata.getWorkerGroup())) {
                continue;
            }
            Set<String> supportedJobCodes = metadata.getSupportedJobCodes();
            if (supportedJobCodes != null && !supportedJobCodes.isEmpty()
                    && !supportedJobCodes.contains(normalizedJobCode)) {
                continue;
            }
            return Optional.ofNullable(handlerBeans.get(entry.getKey()));
        }
        return Optional.empty();
    }

    private String key(String appCode, String serviceCode, String workerGroup, String handlerName) {
        return appCode + ":" + serviceCode + ":" + workerGroup + ":" + handlerName;
    }

    private Set<String> normalizedJobCodes(Set<String> jobCodes) {
        if (jobCodes == null || jobCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String jobCode : jobCodes) {
            String value = MangoJobHandlerSupport.trimToNull(jobCode);
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized;
    }
}
