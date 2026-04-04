package io.mango.i18n.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.mango.i18n.api.entity.SysI18n;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.i18n.core.service.ISysI18nService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * i18n service implementation
 *
 * @author Mango
 */
@Service
public class SysI18nServiceImpl implements ISysI18nService {

    private static final String ZH_CN = "zh-cn";
    private static final String EN = "en";

    /** Cache TTL in milliseconds (default 5 minutes) */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    @Autowired
    private SysI18nMapper sysI18nMapper;

    /** Cached i18n list, refreshed after TTL */
    private volatile List<SysI18n> cachedList;
    private volatile long cacheTimestamp;
    private final ScheduledExecutorService cacheRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "i18n-cache-refresh");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        refreshCache();
        // Schedule periodic cache refresh
        cacheRefreshExecutor.scheduleAtFixedRate(this::refreshCache, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        cacheRefreshExecutor.shutdown();
        try {
            if (!cacheRefreshExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheRefreshExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheRefreshExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void refreshCache() {
        try {
            List<SysI18n> fresh = sysI18nMapper.selectList(Wrappers.emptyWrapper());
            cachedList = fresh;
            cacheTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            // Log but don't fail - keep serving stale cache
        }
    }

    private List<SysI18n> getCachedList() {
        List<SysI18n> list = cachedList;
        if (list == null || System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS) {
            refreshCache();
            list = cachedList;
        }
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public Map<String, List<Map<String, String>>> listMap() {
        List<SysI18n> list = getCachedList();

        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put(ZH_CN, new ArrayList<>());
        result.put(EN, new ArrayList<>());

        for (SysI18n item : list) {
            Map<String, String> zhMap = new HashMap<>();
            zhMap.put(item.getName(), item.getZhCn());
            result.get(ZH_CN).add(zhMap);

            Map<String, String> enMap = new HashMap<>();
            enMap.put(item.getName(), item.getEn());
            result.get(EN).add(enMap);
        }

        return result;
    }

    @Override
    public List<Map<String, String>> listByLang(String lang) {
        List<SysI18n> list = getCachedList();

        boolean isZhCn = ZH_CN.equalsIgnoreCase(lang);
        String field = isZhCn ? "zhCn" : "en";

        return list.stream().map(item -> {
            Map<String, String> map = new HashMap<>();
            String value = isZhCn ? item.getZhCn() : item.getEn();
            // Return the key name when translation is missing, caller can detect this
            map.put(item.getName(), value != null ? value : item.getName());
            return map;
        }).toList();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList(ZH_CN, EN);
    }

    @Override
    @SuppressWarnings("DesignForExtension")
    public SysI18n getByName(final String name) {
        List<SysI18n> list = sysI18nMapper.selectList(
            Wrappers.<SysI18n>query().eq("name", name).last("LIMIT 2"));
        if (list.size() > 1) {
            throw new IllegalStateException("Multiple i18n entries found for name: " + name + ". Ensure 'name' column has a UNIQUE constraint.");
        }
        return list.isEmpty() ? null : list.get(0);
    }
}
