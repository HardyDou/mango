package io.mango.home.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.core.entity.UserHomePageEntity;
import io.mango.home.core.entity.UserHomePreferenceEntity;
import io.mango.home.core.mapper.UserHomePageMapper;
import io.mango.home.core.mapper.UserHomePreferenceMapper;
import io.mango.home.core.service.IHomePageService;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomePageService implements IHomePageService {

    private static final String BUILT_IN_NAME = "系统工作台";
    private static final String DUPLICATE_SUFFIX = " 副本";
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ITEMS = 100;
    private static final int MAX_COLUMNS = 12;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COORDINATE = 999;
    private static final int DEFAULT_SORT_STEP = 10;
    private static final int BAD_REQUEST_CODE = 400;

    private final UserHomePageMapper homePageMapper;
    private final UserHomePreferenceMapper preferenceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<HomePageVO> listMyPages() {
        Long defaultId = currentDefaultHomePageId();
        List<UserHomePageEntity> pages = listEnabledEntities();
        List<HomePageVO> result = new ArrayList<>(pages.size());
        for (UserHomePageEntity page : pages) {
            result.add(toVO(page, isSameId(defaultId, page.getId())));
        }
        return result;
    }

    @Override
    public HomePageVO resolve(ResolveHomePageQuery query) {
        if (query != null && query.getHomeId() != null) {
            UserHomePageEntity specified = selectOwnedEnabled(query.getHomeId());
            Require.notNull(specified, "首页不存在或无权访问");
            return toVO(specified, isSameId(currentDefaultHomePageId(), specified.getId()));
        }
        UserHomePageEntity resolved = resolveDefaultEntity();
        if (resolved == null) {
            return builtInDefault();
        }
        return toVO(resolved, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO create(CreateHomePageCommand command) {
        Require.notNull(command, "创建命令不能为空");
        Require.notBlank(command.getName(), "首页名称不能为空");
        String layoutJson = normalizeLayoutJson(command.getLayoutJson());
        int nextSort = nextSort();
        UserHomePageEntity entity = new UserHomePageEntity();
        entity.setTenantId(HomeContextSupport.currentTenantId());
        entity.setOrgId(HomeContextSupport.currentOrgId());
        entity.setUserId(HomeContextSupport.currentUserId());
        entity.setName(command.getName().trim());
        entity.setLayoutJson(layoutJson);
        entity.setSort(nextSort);
        entity.setEnabled(true);
        boolean setAsDefault = Boolean.TRUE.equals(command.getSetDefault()) || currentDefaultHomePageId() == null;
        homePageMapper.insert(entity);
        if (setAsDefault) {
            saveDefaultHomePageId(entity.getId());
        }
        return toVO(entity, setAsDefault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO rename(Long id, RenameHomePageCommand command) {
        Require.notNull(command, "重命名命令不能为空");
        Require.notBlank(command.getName(), "首页名称不能为空");
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setName(command.getName().trim());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isSameId(currentDefaultHomePageId(), entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO duplicate(Long id) {
        UserHomePageEntity source = requiredOwnedEnabled(id);
        UserHomePageEntity entity = new UserHomePageEntity();
        entity.setTenantId(HomeContextSupport.currentTenantId());
        entity.setOrgId(HomeContextSupport.currentOrgId());
        entity.setUserId(HomeContextSupport.currentUserId());
        entity.setName(source.getName() + DUPLICATE_SUFFIX);
        entity.setLayoutJson(source.getLayoutJson());
        entity.setSort(nextSort());
        entity.setEnabled(true);
        homePageMapper.insert(entity);
        return toVO(entity, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO saveLayout(Long id, SaveHomePageLayoutCommand command) {
        Require.notNull(command, "布局保存命令不能为空");
        Require.notBlank(command.getLayoutJson(), "layoutJson不能为空");
        validateLayoutJson(command.getLayoutJson());
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setLayoutJson(command.getLayoutJson());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isSameId(currentDefaultHomePageId(), entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HomePageVO> sort(SortHomePagesCommand command) {
        Require.notNull(command, "排序命令不能为空");
        Require.notEmpty(command.getIds(), "首页排序不能为空");
        List<UserHomePageEntity> pages = listEnabledEntities();
        Set<Long> ownedIds = new HashSet<>();
        for (UserHomePageEntity page : pages) {
            ownedIds.add(page.getId());
        }
        int sort = DEFAULT_SORT_STEP;
        for (Long id : command.getIds()) {
            Require.isTrue(ownedIds.contains(id), "首页排序包含无权访问的数据");
            UserHomePageEntity entity = requiredOwnedEnabled(id);
            entity.setSort(sort);
            entity.setUpdatedBy(MangoContextHolder.userId());
            homePageMapper.updateById(entity);
            sort += DEFAULT_SORT_STEP;
        }
        return listMyPages();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO setDefault(Long id) {
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        saveDefaultHomePageId(entity.getId());
        return toVO(entity, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO delete(Long id) {
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setEnabled(false);
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        if (isSameId(currentDefaultHomePageId(), id)) {
            UserHomePageEntity fallback = firstEnabledEntity();
            saveDefaultHomePageId(fallback == null ? null : fallback.getId());
        }
        return resolve(new ResolveHomePageQuery());
    }

    private String normalizeLayoutJson(String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            return defaultLayoutJson();
        }
        validateLayoutJson(layoutJson);
        return layoutJson;
    }

    private String defaultLayoutJson() {
        return "{\"schemaVersion\":1,\"items\":[]}";
    }

    private void validateLayoutJson(String layoutJson) {
        try {
            JsonNode root = objectMapper.readTree(layoutJson);
            Require.isTrue(root.path("schemaVersion").asInt() == SCHEMA_VERSION, "布局结构版本不支持");
            JsonNode items = root.path("items");
            Require.isTrue(items.isArray(), "布局 items 必须是数组");
            Require.isTrue(items.size() <= MAX_ITEMS, "布局组件数量不能超过100个");
            for (JsonNode item : items) {
                validateItem(item);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            Require.fail(BAD_REQUEST_CODE, "布局 JSON 格式不正确");
        }
    }

    private void validateItem(JsonNode item) {
        Require.notBlank(item.path("id").asText(null), "布局项 id 不能为空");
        Require.notBlank(item.path("widgetType").asText(null), "布局项 widgetType 不能为空");
        JsonNode layout = item.path("layout");
        Require.isTrue(layout.isObject(), "布局项 layout 不能为空");
        int x = layout.path("x").asInt(-1);
        int y = layout.path("y").asInt(-1);
        int w = layout.path("w").asInt(-1);
        int h = layout.path("h").asInt(-1);
        Require.inRange(x, 0, MAX_COORDINATE, "布局项 x 超出范围");
        Require.inRange(y, 0, MAX_COORDINATE, "布局项 y 超出范围");
        Require.inRange(w, 1, MAX_COLUMNS, "布局项 w 超出范围");
        Require.inRange(h, 1, MAX_ROWS, "布局项 h 超出范围");
        Require.isTrue(x + w <= MAX_COLUMNS, "布局项宽度超出12栅格");
        validateOptionalSize(layout, "minW", MAX_COLUMNS);
        validateOptionalSize(layout, "minH", MAX_ROWS);
        validateOptionalSize(layout, "maxW", MAX_COLUMNS);
        validateOptionalSize(layout, "maxH", MAX_ROWS);
    }

    private void validateOptionalSize(JsonNode layout, String fieldName, int maxValue) {
        JsonNode node = layout.get(fieldName);
        if (node != null && !node.isNull()) {
            Require.inRange(node.asInt(-1), 1, maxValue, "布局项 " + fieldName + " 超出范围");
        }
    }

    private UserHomePageEntity requiredOwnedEnabled(Long id) {
        Require.notNull(id, "首页ID不能为空");
        UserHomePageEntity entity = selectOwnedEnabled(id);
        Require.notNull(entity, "首页不存在或无权访问");
        return entity;
    }

    private UserHomePageEntity selectOwnedEnabled(Long id) {
        return homePageMapper.selectOne(baseWrapper()
                .eq(UserHomePageEntity::getId, id)
                .eq(UserHomePageEntity::getEnabled, true));
    }

    private UserHomePageEntity resolveDefaultEntity() {
        Long defaultId = currentDefaultHomePageId();
        if (defaultId != null) {
            UserHomePageEntity entity = selectOwnedEnabled(defaultId);
            if (entity != null) {
                return entity;
            }
        }
        UserHomePageEntity fallback = firstEnabledEntity();
        if (fallback != null) {
            saveDefaultHomePageId(fallback.getId());
        }
        return fallback;
    }

    private UserHomePageEntity firstEnabledEntity() {
        List<UserHomePageEntity> pages = listEnabledEntities();
        if (pages.isEmpty()) {
            return null;
        }
        return pages.get(0);
    }

    private List<UserHomePageEntity> listEnabledEntities() {
        return homePageMapper.selectList(baseWrapper()
                .eq(UserHomePageEntity::getEnabled, true)
                .orderByAsc(UserHomePageEntity::getSort)
                .orderByAsc(UserHomePageEntity::getCreatedAt)
                .orderByAsc(UserHomePageEntity::getId));
    }

    private int nextSort() {
        List<UserHomePageEntity> pages = listEnabledEntities();
        if (pages.isEmpty()) {
            return DEFAULT_SORT_STEP;
        }
        return pages.get(pages.size() - 1).getSort() + DEFAULT_SORT_STEP;
    }

    private LambdaQueryWrapper<UserHomePageEntity> baseWrapper() {
        return new LambdaQueryWrapper<UserHomePageEntity>()
                .eq(UserHomePageEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePageEntity::getUserId, HomeContextSupport.currentUserId());
    }

    private Long currentDefaultHomePageId() {
        UserHomePreferenceEntity preference = preferenceMapper.selectOne(preferenceWrapper());
        if (preference == null) {
            return null;
        }
        return preference.getDefaultHomePageId();
    }

    private void saveDefaultHomePageId(Long homePageId) {
        UserHomePreferenceEntity preference = preferenceMapper.selectOne(preferenceWrapper());
        if (preference == null) {
            preference = new UserHomePreferenceEntity();
            preference.setTenantId(HomeContextSupport.currentTenantId());
            preference.setOrgId(HomeContextSupport.currentOrgId());
            preference.setUserId(HomeContextSupport.currentUserId());
            preference.setDefaultHomePageId(homePageId);
            preferenceMapper.insert(preference);
            return;
        }
        preference.setDefaultHomePageId(homePageId);
        preference.setUpdatedBy(MangoContextHolder.userId());
        preferenceMapper.updateById(preference);
    }

    private LambdaQueryWrapper<UserHomePreferenceEntity> preferenceWrapper() {
        return new LambdaQueryWrapper<UserHomePreferenceEntity>()
                .eq(UserHomePreferenceEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePreferenceEntity::getUserId, HomeContextSupport.currentUserId());
    }

    private HomePageVO builtInDefault() {
        HomePageVO vo = new HomePageVO();
        vo.setName(BUILT_IN_NAME);
        vo.setLayoutJson(defaultLayoutJson());
        vo.setSort(0);
        vo.setEnabled(true);
        vo.setDefaultPage(true);
        vo.setBuiltIn(true);
        return vo;
    }

    private HomePageVO toVO(UserHomePageEntity entity, boolean defaultPage) {
        HomePageVO vo = new HomePageVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setName(entity.getName());
        vo.setLayoutJson(entity.getLayoutJson());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setDefaultPage(defaultPage);
        vo.setBuiltIn(false);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private boolean isSameId(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
