package io.mango.job.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 当前应用内 Job 处理器注册表。
 */
@Service
public class MangoJobHandlerRegistry implements IMangoJobHandlerRegistry {

    private final Map<String, MangoJobHandlerVO> handlers = new LinkedHashMap<>();

    private final Map<String, MangoJobHandler> handlerBeans = new LinkedHashMap<>();

    public MangoJobHandlerRegistry(ObjectProvider<MangoJobHandler> provider) {
        provider.orderedStream().forEach(this::register);
    }

    @Override
    public synchronized void register(MangoJobHandler handler) {
        Require.notNull(handler, "Job 处理器不能为空");
        String handlerName = MangoJobSupport.normalizeRequired(handler.handlerName(), "Job 处理器名称不能为空");
        String appCode = MangoJobSupport.trimToNull(MangoContextHolder.appCode());
        if (appCode == null) {
            appCode = "local";
        }
        String key = appCode + ":" + handlerName;
        Require.isTrue(!handlers.containsKey(key), "Job 处理器名称重复：" + handlerName);

        MangoJobHandlerVO vo = new MangoJobHandlerVO();
        vo.setAppCode(appCode);
        vo.setHandlerName(handlerName);
        vo.setJobType(JobType.BUILTIN.name());
        vo.setConcurrent(Boolean.TRUE);
        handlers.put(key, vo);
        handlerBeans.put(key, handler);
    }

    @Override
    public synchronized List<MangoJobHandlerVO> listHandlers() {
        return handlers.values().stream()
                .sorted(Comparator.comparing(MangoJobHandlerVO::getAppCode)
                .thenComparing(MangoJobHandlerVO::getHandlerName))
                .toList();
    }

    @Override
    public synchronized Optional<MangoJobHandler> findHandler(String handlerName) {
        return findHandler(MangoContextHolder.appCode(), handlerName);
    }

    @Override
    public synchronized Optional<MangoJobHandler> findHandler(String appCode, String handlerName) {
        String normalized = MangoJobSupport.trimToNull(handlerName);
        if (normalized == null) {
            return Optional.empty();
        }
        String normalizedAppCode = MangoJobSupport.trimToNull(appCode);
        if (normalizedAppCode != null) {
            MangoJobHandler handler = handlerBeans.get(normalizedAppCode + ":" + normalized);
            if (handler != null) {
                return Optional.of(handler);
            }
        }
        return Optional.ofNullable(handlerBeans.get("local:" + normalized));
    }
}
