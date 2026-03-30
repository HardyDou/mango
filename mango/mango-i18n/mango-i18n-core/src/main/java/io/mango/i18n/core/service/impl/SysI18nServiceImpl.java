package io.mango.i18n.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.mango.i18n.api.entity.SysI18n;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.i18n.api.service.SysI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * i18n service implementation
 *
 * @author Mango
 */
@Service
public class SysI18nServiceImpl implements SysI18nService {

    @Autowired
    private SysI18nMapper sysI18nMapper;

    @Override
    public Map<String, List<Map<String, String>>> listMap() {
        List<SysI18n> list = sysI18nMapper.selectList(Wrappers.emptyWrapper());

        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("zh-cn", new ArrayList<>());
        result.put("en", new ArrayList<>());

        for (SysI18n item : list) {
            Map<String, String> zhMap = new HashMap<>();
            zhMap.put(item.getName(), item.getZhCn());
            result.get("zh-cn").add(zhMap);

            Map<String, String> enMap = new HashMap<>();
            enMap.put(item.getName(), item.getEn());
            result.get("en").add(enMap);
        }

        return result;
    }

    @Override
    public List<Map<String, String>> listByLang(String lang) {
        List<SysI18n> list = sysI18nMapper.selectList(Wrappers.emptyWrapper());

        String field = "zh-cn".equalsIgnoreCase(lang) ? "zhCn" : "en";

        return list.stream().map(item -> {
            Map<String, String> map = new HashMap<>();
            String value = "zh-cn".equalsIgnoreCase(lang) ? item.getZhCn() : item.getEn();
            map.put(item.getName(), value != null ? value : item.getName());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList("zh-cn", "en");
    }

    @Override
    public SysI18n getByName(String name) {
        return sysI18nMapper.selectOne(Wrappers.<SysI18n>lambdaQuery().eq(SysI18n::getName, name));
    }
}
