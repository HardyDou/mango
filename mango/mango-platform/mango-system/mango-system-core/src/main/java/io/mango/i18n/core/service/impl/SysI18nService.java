package io.mango.i18n.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.mango.common.result.Require;
import io.mango.i18n.api.vo.SysI18nMessageVO;
import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import io.mango.i18n.core.entity.SysI18nEntity;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.i18n.core.service.ISysI18nService;
import io.mango.infra.context.support.TtlExecutorDecorator;
import io.mango.i18n.api.enums.I18nCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysI18nService implements ISysI18nService {

    private static final String ZH_CN = "zh-cn";
    private static final String EN = "en";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final SysI18nMapper sysI18nMapper;
    private final TtlExecutorDecorator ttlExecutorDecorator;

    private volatile List<SysI18nEntity> cachedList;
    private volatile long cacheTimestamp;
    private ScheduledExecutorService cacheRefreshExecutor;

    @PostConstruct
    public void init() {
        refreshCache();
        cacheRefreshExecutor = ttlExecutorDecorator.decorate(Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "i18n-cache-refresh");
            thread.setDaemon(true);
            return thread;
        }));
        cacheRefreshExecutor.scheduleAtFixedRate(this::refreshCache, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        if (cacheRefreshExecutor == null) {
            return;
        }
        cacheRefreshExecutor.shutdown();
        try {
            if (!cacheRefreshExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                cacheRefreshExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            cacheRefreshExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public I18nLanguagePackVO listMap() {
        return I18nLanguagePackVO.all(listByLang(ZH_CN), listByLang(EN));
    }

    @Override
    public List<I18nEntryVO> listByLang(String lang) {
        boolean chinese = ZH_CN.equalsIgnoreCase(lang) || "zh_CN".equalsIgnoreCase(lang);
        return getCachedList().stream()
                .map(item -> new I18nEntryVO(
                        item.getName(), fallback(chinese ? item.getZhCn() : item.getEn(), item.getName())))
                .toList();
    }

    @Override
    public I18nLanguagePackVO languagePack(String lang) {
        return I18nLanguagePackVO.single(lang, listByLang(lang));
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList(ZH_CN, EN);
    }

    @Override
    public SysI18nMessageVO getByName(String name) {
        List<SysI18nEntity> entries = sysI18nMapper.selectList(
                Wrappers.<SysI18nEntity>query().eq("name", name).last("LIMIT 2"));
        Require.isTrue(entries.size() <= 1, I18nCode.I18N_INVALID,
                "Multiple i18n entries found for name: " + name + ". Ensure 'name' column has a UNIQUE constraint.");
        Require.isTrue(!entries.isEmpty(), I18nCode.I18N_MESSAGE_NOT_FOUND);
        return toVO(entries.getFirst());
    }

    private void refreshCache() {
        try {
            cachedList = List.copyOf(sysI18nMapper.selectList(Wrappers.emptyWrapper()));
            cacheTimestamp = System.currentTimeMillis();
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh i18n cache; stale cache remains active", exception);
        }
    }

    private List<SysI18nEntity> getCachedList() {
        List<SysI18nEntity> list = cachedList;
        if (list == null || list.isEmpty() || System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS) {
            refreshCache();
            list = cachedList;
        }
        return list == null ? Collections.emptyList() : list;
    }

    private SysI18nMessageVO toVO(SysI18nEntity entity) {
        SysI18nMessageVO vo = new SysI18nMessageVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setZhCn(entity.getZhCn());
        vo.setEn(entity.getEn());
        vo.setDescription(entity.getDescription());
        return vo;
    }

    private String fallback(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
