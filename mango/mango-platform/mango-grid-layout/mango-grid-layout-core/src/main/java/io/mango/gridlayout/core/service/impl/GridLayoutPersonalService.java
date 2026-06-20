package io.mango.gridlayout.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;
import io.mango.gridlayout.core.entity.MangoUserGridLayoutEntity;
import io.mango.gridlayout.core.mapper.MangoUserGridLayoutMapper;
import io.mango.gridlayout.core.service.IGridLayoutPersonalService;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GridLayoutPersonalService implements IGridLayoutPersonalService {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ITEMS = 100;
    private static final int MAX_COLUMNS = 12;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COORDINATE = 999;

    private final MangoUserGridLayoutMapper gridLayoutMapper;
    private final ObjectMapper objectMapper;

    @Override
    public GridLayoutPersonalVO getPersonal(GridLayoutPersonalQuery query) {
        Require.notNull(query, "查询条件不能为空");
        Require.notBlank(query.getPageCode(), "pageCode不能为空");
        MangoUserGridLayoutEntity entity = gridLayoutMapper.selectOne(exactWrapper(query.getPageCode()));
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GridLayoutPersonalVO savePersonal(SaveGridLayoutPersonalCommand command) {
        Require.notNull(command, "保存命令不能为空");
        Require.notBlank(command.getPageCode(), "pageCode不能为空");
        Require.notBlank(command.getLayoutJson(), "layoutJson不能为空");
        validateLayoutJson(command.getPageCode(), command.getLayoutJson());

        MangoUserGridLayoutEntity entity = gridLayoutMapper.selectOne(exactWrapper(command.getPageCode()));
        if (entity == null) {
            entity = new MangoUserGridLayoutEntity();
            entity.setTenantId(GridLayoutContextSupport.currentTenantId());
            entity.setUserId(GridLayoutContextSupport.currentUserId());
            entity.setPageCode(command.getPageCode());
            fillMutableFields(entity, command.getLayoutJson());
            gridLayoutMapper.insert(entity);
        } else {
            fillMutableFields(entity, command.getLayoutJson());
            gridLayoutMapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePersonal(GridLayoutPersonalQuery query) {
        Require.notNull(query, "删除条件不能为空");
        Require.notBlank(query.getPageCode(), "pageCode不能为空");
        return gridLayoutMapper.delete(exactWrapper(query.getPageCode())) > 0;
    }

    private void validateLayoutJson(String pageCode, String layoutJson) {
        try {
            JsonNode root = objectMapper.readTree(layoutJson);
            Require.isTrue(root.path("schemaVersion").asInt() == SCHEMA_VERSION, "布局结构版本不支持");
            Require.isTrue(pageCode.equals(root.path("pageCode").asText()), "布局页面编码不一致");
            JsonNode items = root.path("items");
            Require.isTrue(items.isArray(), "布局 items 必须是数组");
            Require.isTrue(items.size() <= MAX_ITEMS, "布局组件数量不能超过100个");
            for (JsonNode item : items) {
                validateItem(item);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            Require.fail(400, "布局 JSON 格式不正确");
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

    private void fillMutableFields(MangoUserGridLayoutEntity entity, String layoutJson) {
        entity.setSchemaVersion(SCHEMA_VERSION);
        entity.setLayoutJson(layoutJson);
        entity.setUpdatedBy(MangoContextHolder.userId());
    }

    private LambdaQueryWrapper<MangoUserGridLayoutEntity> exactWrapper(String pageCode) {
        return new LambdaQueryWrapper<MangoUserGridLayoutEntity>()
                .eq(MangoUserGridLayoutEntity::getTenantId, GridLayoutContextSupport.currentTenantId())
                .eq(MangoUserGridLayoutEntity::getUserId, GridLayoutContextSupport.currentUserId())
                .eq(MangoUserGridLayoutEntity::getPageCode, pageCode);
    }

    private GridLayoutPersonalVO toVO(MangoUserGridLayoutEntity entity) {
        GridLayoutPersonalVO vo = new GridLayoutPersonalVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setPageCode(entity.getPageCode());
        vo.setSchemaVersion(entity.getSchemaVersion());
        vo.setLayoutJson(entity.getLayoutJson());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
