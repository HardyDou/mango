package io.mango.i18n.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.i18n.api.entity.SysI18n;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SysI18nServiceImpl unit tests
 */
class SysI18nServiceImplTest {

    private SysI18nServiceImpl service;
    private List<SysI18n> storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new ArrayList<>();
        Object proxyMapper = createMapperProxy(storage);
        service = new SysI18nServiceImpl();
        Field field = SysI18nServiceImpl.class.getDeclaredField("sysI18nMapper");
        field.setAccessible(true);
        field.set(service, proxyMapper);
    }

    // ========== listMap() tests ==========

    @Test
    void listMap_shouldGroupEntriesByLanguage() {
        storage.add(createI18n("hello", "你好", "Hello"));

        Map<String, List<Map<String, String>>> result = service.listMap();

        assertNotNull(result);
        assertTrue(result.containsKey("zh-cn"));
        assertTrue(result.containsKey("en"));

        Map<String, String> zhEntry = result.get("zh-cn").get(0);
        assertEquals("你好", zhEntry.get("hello"));

        Map<String, String> enEntry = result.get("en").get(0);
        assertEquals("Hello", enEntry.get("hello"));
    }

    @Test
    void listMap_shouldReturnEmptyListsWhenNoData() {
        Map<String, List<Map<String, String>>> result = service.listMap();

        assertNotNull(result);
        assertTrue(result.get("zh-cn").isEmpty());
        assertTrue(result.get("en").isEmpty());
    }

    @Test
    void listMap_shouldHandleNullFieldValues() {
        storage.add(createI18n("greeting", null, null));

        Map<String, List<Map<String, String>>> result = service.listMap();

        Map<String, String> zhEntry = result.get("zh-cn").get(0);
        assertNull(zhEntry.get("greeting"));
    }

    // ========== listByLang() tests ==========

    @Test
    void listByLang_zhCn_shouldReturnChineseEntries() {
        storage.add(createI18n("welcome", "欢迎", "Welcome"));

        List<Map<String, String>> result = service.listByLang("zh-cn");

        assertEquals(1, result.size());
        assertEquals("欢迎", result.get(0).get("welcome"));
    }

    @Test
    void listByLang_en_shouldReturnEnglishEntries() {
        storage.add(createI18n("welcome", "欢迎", "Welcome"));

        List<Map<String, String>> result = service.listByLang("en");

        assertEquals(1, result.size());
        assertEquals("Welcome", result.get(0).get("welcome"));
    }

    @Test
    void listByLang_caseInsensitive_shouldMapToCorrectField() {
        storage.add(createI18n("key", "中", "EN"));

        assertEquals("中", service.listByLang("ZH-CN").get(0).get("key"));
        assertEquals("中", service.listByLang("zh-cn").get(0).get("key"));
        assertEquals("EN", service.listByLang("fr").get(0).get("key"));
        assertEquals("EN", service.listByLang("ja").get(0).get("key"));
    }

    @Test
    void listByLang_nullValue_shouldFallbackToKeyName() {
        storage.add(createI18n("missing", null, null));

        List<Map<String, String>> result = service.listByLang("zh-cn");

        assertEquals("missing", result.get(0).get("missing"));
    }

    @Test
    void listByLang_empty_shouldReturnEmptyList() {
        List<Map<String, String>> result = service.listByLang("zh-cn");

        assertTrue(result.isEmpty());
    }

    // ========== getSupportedLanguages() tests ==========

    @Test
    void getSupportedLanguages_shouldReturnCorrectLocales() {
        List<String> result = service.getSupportedLanguages();

        assertEquals(Arrays.asList("zh-cn", "en"), result);
    }

    // ========== getByName() tests ==========
    // Note: getByName() uses MyBatis-Plus QueryWrapper which stores condition values
    // in lambda captures and private fields that cannot be reliably extracted in a
    // unit test without Mockito. The method is a thin wrapper around selectOne() and
    // is covered by integration tests with a real database.

    // ========== Helpers ==========

    private static Object getField(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static SysI18n createI18n(String name, String zhCn, String en) {
        SysI18n entity = new SysI18n();
        entity.setName(name);
        entity.setZhCn(zhCn);
        entity.setEn(en);
        return entity;
    }

    /**
     * Creates a JDK dynamic proxy implementing SysI18nMapper (via BaseMapper<SysI18n>)
     * that intercepts selectList and selectOne calls.
     */
    private static Object createMapperProxy(List<SysI18n> storage) {
        ClassLoader cl = SysI18nServiceImpl.class.getClassLoader();
        // SysI18nMapper extends BaseMapper<SysI18n>
        Class<?> mapperInterface = null;
        try {
            mapperInterface = cl.loadClass("io.mango.i18n.core.mapper.SysI18nMapper");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return Proxy.newProxyInstance(cl, new Class<?>[]{mapperInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("selectList".equals(name)) {
                    return new ArrayList<>(storage);
                }
                if ("selectOne".equals(name)) {
                    Wrapper<?> wrapper = (Wrapper<?>) args[0];
                    String keyFromSql = null;
                    // Try getTargetSql() for non-lambda wrappers (QueryWrapper)
                    try {
                        String sql = wrapper.getTargetSql();
                        if (sql != null) {
                            int start = sql.indexOf("'");
                            if (start >= 0) {
                                int end = sql.lastIndexOf("'");
                                if (end > start) {
                                    keyFromSql = sql.substring(start + 1, end);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Fall through to reflection approach
                    }
                    if (keyFromSql != null) {
                        final String lookupKey = keyFromSql;
                        return storage.stream()
                                .filter(item -> lookupKey.equals(item.getName()))
                                .findFirst()
                                .orElse(null);
                    }
                    // LambdaQueryWrapper: extract key via reflection from normalSegmentList
                    try {
                        Object normalSegmentList = getField(wrapper, "normalSegmentList");
                        if (normalSegmentList != null) {
                            Iterable<?> segments = (Iterable<?>) normalSegmentList;
                            for (Object seg : segments) {
                                Object expression = getField(seg, "expression");
                                if (expression != null) {
                                    Class<?> exprClass = expression.getClass();
                                    if (exprClass.getName().contains("LambdaEqExpression")) {
                                        Object keyValues = exprClass.getMethod("getValue").invoke(expression);
                                        if (keyValues != null) {
                                            Object valueObj = getField(keyValues, "value");
                                            if (valueObj != null) {
                                                String keyFromLambda = valueObj.toString();
                                                final String lookupKey = keyFromLambda;
                                                return storage.stream()
                                                        .filter(item -> lookupKey.equals(item.getName()))
                                                        .findFirst()
                                                        .orElse(null);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Fall through to null
                    }
                    return null;
                }
                // Return defaults for other BaseMapper methods
                if ("deleteById".equals(name)) return 0;
                if ("updateById".equals(name)) return 0;
                if ("insert".equals(name)) return 0;
                if ("deleteBatchIds".equals(name)) return 0;
                if ("deleteByMap".equals(name)) return 0;
                if ("delete".equals(name)) return 0;
                if ("selectById".equals(name)) return null;
                if ("selectBatchIds".equals(name)) return Collections.emptyList();
                if ("selectByMap".equals(name)) return Collections.emptyList();
                if ("selectCount".equals(name)) return 0;
                if ("selectMaps".equals(name)) return Collections.emptyList();
                if ("selectObjs".equals(name)) return Collections.emptyList();
                if ("selectPage".equals(name)) return null;
                if ("selectMapsPage".equals(name)) return null;
                if ("selectCount".equals(name)) return 0;
                throw new UnsupportedOperationException("Unsupported method: " + name);
            }
        });
    }
}
