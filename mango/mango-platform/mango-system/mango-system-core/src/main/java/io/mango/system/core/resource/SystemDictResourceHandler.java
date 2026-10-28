package io.mango.system.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.system.api.enums.DictShapeEnum;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import io.mango.system.core.entity.DictDataEntity;
import io.mango.system.core.entity.DictTypeEntity;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统字典资源处理器。
 */
@Component
@RequiredArgsConstructor
public class SystemDictResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_dict_type";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.SYSTEM_DICT;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("dictType")
                .requiredField("dictName")
                .requiredField("items")
                .fieldDescription("typeId", "字典类型表稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("dictType", "字典类型编码，全局唯一。")
                .fieldDescription("dictName", "字典类型名称。")
                .fieldDescription("shape", "字典形态：FLAT 或 TREE，默认 FLAT。")
                .fieldDescription("domainCode", "业务域编码，默认 COMMON。")
                .fieldDescription("status", "状态：1 启用，0 禁用。")
                .fieldDescription("remark", "字典类型备注。")
                .fieldDescription("items", "字典项列表，每项支持 id、label、value、parentValue、sort、status、remark。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        DictPayload payload = DictPayload.from(resource);
        validatePayload(payload);
        DictTypeEntity type = findType(payload.dictType());
        if (type == null) {
            type = new DictTypeEntity();
            type.setId(payload.typeId());
            type.setDictType(payload.dictType());
            applyType(type, payload);
            dictTypeMapper.insert(type);
        } else {
            applyType(type, payload);
            dictTypeMapper.updateById(type);
        }

        Set<Long> activeItemIds = new HashSet<>();
        Set<String> activeItemValues = new HashSet<>();
        for (DictItemPayload item : payload.items()) {
            DictDataEntity data = findData(payload.dictType(), item);
            if (data == null) {
                data = new DictDataEntity();
                data.setId(item.id());
                data.setDictType(payload.dictType());
                applyData(data, item);
                dictDataMapper.insert(data);
            } else {
                data.setDictType(payload.dictType());
                applyData(data, item);
                dictDataMapper.updateById(data);
            }
            if (data.getId() != null) {
                activeItemIds.add(data.getId());
            }
            activeItemValues.add(item.value());
        }
        disableRemovedItems(payload.dictType(), activeItemIds, activeItemValues);
        return ResourceSyncResult.of(type.getId(), TARGET_TABLE, "System dict synced: " + payload.dictType());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String dictType = fieldText(resource, "dictType", false);
        DictTypeEntity type = resolveType(resource, dictType);
        if (type == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System dict not found: " + dictType);
        }
        dictType = type.getDictType();
        type.setStatus(DISABLED);
        dictTypeMapper.updateById(type);

        DictDataEntity data = new DictDataEntity();
        data.setStatus(DISABLED);
        data.setUpdatedAt(LocalDateTime.now());
        dictDataMapper.update(data, new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictType, dictType));
        return ResourceSyncResult.of(type.getId(), TARGET_TABLE, "System dict disabled: " + dictType);
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        String dictType = fieldText(resource, "dictType", false);
        DictTypeEntity type = resolveType(resource, dictType);
        if (type == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System dict not found: " + dictType);
        }
        dictType = type.getDictType();
        dictDataMapper.delete(new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictType, dictType));
        dictTypeMapper.deleteById(type.getId());
        return ResourceSyncResult.of(type.getId(), TARGET_TABLE, "System dict deleted: " + dictType);
    }

    private void applyType(DictTypeEntity type, DictPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        type.setDictName(payload.dictName());
        type.setShape(payload.shape());
        type.setDomainCode(payload.domainCode());
        type.setStatus(payload.status());
        type.setRemark(payload.remark());
        if (!StringUtils.hasText(type.getTenantId())) {
            type.setTenantId(DEFAULT_TENANT_ID);
        }
        if (type.getCreatedAt() == null) {
            type.setCreatedAt(now);
        }
        type.setUpdatedAt(now);
    }

    private void applyData(DictDataEntity data, DictItemPayload item) {
        LocalDateTime now = LocalDateTime.now();
        data.setDictLabel(item.label());
        data.setDictValue(item.value());
        data.setParentValue(item.parentValue());
        data.setSort(item.sort());
        data.setStatus(item.status());
        data.setRemark(item.remark());
        if (!StringUtils.hasText(data.getTenantId())) {
            data.setTenantId(DEFAULT_TENANT_ID);
        }
        if (data.getCreatedAt() == null) {
            data.setCreatedAt(now);
        }
        data.setUpdatedAt(now);
    }

    private void disableRemovedItems(String dictType, Set<Long> activeItemIds, Set<String> activeItemValues) {
        List<DictDataEntity> existing = dictDataMapper.selectList(
                new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictType, dictType));
        for (DictDataEntity data : existing) {
            boolean declaredById = data.getId() != null && activeItemIds.contains(data.getId());
            boolean declaredByValue = activeItemValues.contains(data.getDictValue());
            if (!declaredById && !declaredByValue && !Integer.valueOf(DISABLED).equals(data.getStatus())) {
                data.setStatus(DISABLED);
                dictDataMapper.updateById(data);
            }
        }
    }

    private DictTypeEntity findType(String dictType) {
        return dictTypeMapper.selectOne(new LambdaQueryWrapper<DictTypeEntity>()
                .eq(DictTypeEntity::getDictType, dictType)
                .last("limit 1"));
    }

    private DictTypeEntity findTypeByTargetId(ResourceDeclaration resource) {
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId == null) {
            return null;
        }
        return dictTypeMapper.selectById(targetId);
    }

    private DictTypeEntity resolveType(ResourceDeclaration resource, String dictType) {
        if (StringUtils.hasText(dictType)) {
            return findType(dictType);
        }
        return findTypeByTargetId(resource);
    }

    private DictDataEntity findData(String dictType, DictItemPayload item) {
        if (item.id() != null) {
            DictDataEntity byId = dictDataMapper.selectById(item.id());
            if (byId != null) {
                return byId;
            }
        }
        return dictDataMapper.selectOne(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictType, dictType)
                .eq(DictDataEntity::getDictValue, item.value())
                .last("limit 1"));
    }

    private record DictPayload(Long typeId, String dictType, String dictName, String shape, String domainCode, Integer status,
                               String remark, List<DictItemPayload> items) {

        private static DictPayload from(ResourceDeclaration resource) {
            String dictType = fieldText(resource, "dictType", true);
            return new DictPayload(
                    fieldLong(resource, "typeId", false, Long.valueOf(resource.getId())),
                    dictType,
                    fieldText(resource, "dictName", true),
                    normalizeShape(fieldText(resource, "shape", false)),
                    defaultText(fieldText(resource, "domainCode", false), "COMMON"),
                    fieldInt(resource, "status", false, ENABLED),
                    fieldText(resource, "remark", false),
                    fieldList(resource, "items").stream()
                            .map(DictItemPayload::from)
                            .toList()
            );
        }
    }

    private record DictItemPayload(Long id, String label, String value, String parentValue, Integer sort, Integer status, String remark) {

        private static DictItemPayload from(Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalStateException("SYSTEM_DICT items must be object list");
            }
            return new DictItemPayload(
                    toLong(map.get("id"), false, null),
                    requiredText(map.get("label"), "SYSTEM_DICT item label is required"),
                    requiredText(map.get("value"), "SYSTEM_DICT item value is required"),
                    toText(map.get("parentValue")),
                    toInt(map.get("sort"), false, 0),
                    toInt(map.get("status"), false, ENABLED),
                    toText(map.get("remark"))
            );
        }
    }

    private static void validatePayload(DictPayload payload) {
        Map<String, DictItemPayload> itemsByValue = new HashMap<>();
        for (DictItemPayload item : payload.items()) {
            if (itemsByValue.put(item.value(), item) != null) {
                throw new IllegalStateException("SYSTEM_DICT item value must be unique: " + item.value());
            }
            if (DictShapeEnum.FLAT.getCode().equals(payload.shape()) && item.parentValue() != null) {
                throw new IllegalStateException("FLAT SYSTEM_DICT item cannot have parentValue: " + item.value());
            }
        }
        if (DictShapeEnum.TREE.getCode().equals(payload.shape())) {
            for (DictItemPayload item : payload.items()) {
                Set<String> visited = new HashSet<>();
                String cursor = item.parentValue();
                while (cursor != null && !cursor.isBlank()) {
                    if (!visited.add(cursor) || cursor.equals(item.value())) {
                        throw new IllegalStateException("SYSTEM_DICT item has cyclic parentValue: " + item.value());
                    }
                    DictItemPayload parent = itemsByValue.get(cursor);
                    if (parent == null) {
                        throw new IllegalStateException("SYSTEM_DICT parentValue does not exist: " + cursor);
                    }
                    cursor = parent.parentValue();
                }
            }
        }
    }

    private static String normalizeShape(String shape) {
        DictShapeEnum normalized = DictShapeEnum.fromCode(shape);
        if (normalized == null) {
            throw new IllegalStateException("SYSTEM_DICT shape must be FLAT or TREE");
        }
        return normalized.getCode();
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static List<?> fieldList(ResourceDeclaration resource, String name) {
        Object value = fieldValue(resource, name, true);
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("SYSTEM_DICT field must be list: " + name);
        }
        return list;
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = null;
        if (field != null) {
            value = field.getValue();
        }
        if (required && value == null) {
            throw new IllegalStateException("SYSTEM_DICT field is required: " + name);
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return defaultValue;
    }

    private static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static String toText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("SYSTEM_DICT long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("SYSTEM_DICT int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
