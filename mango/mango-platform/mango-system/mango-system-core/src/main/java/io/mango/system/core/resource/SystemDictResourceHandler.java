package io.mango.system.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.system.core.entity.DictData;
import io.mango.system.core.entity.DictType;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
                .fieldDescription("domainCode", "业务域编码，默认 COMMON。")
                .fieldDescription("status", "状态：1 启用，0 禁用。")
                .fieldDescription("remark", "字典类型备注。")
                .fieldDescription("items", "字典项列表，每项支持 id、label、value、sort、status、remark。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        DictPayload payload = DictPayload.from(resource);
        DictType type = findType(payload.dictType());
        if (type == null) {
            type = new DictType();
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
            DictData data = findData(payload.dictType(), item);
            if (data == null) {
                data = new DictData();
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
        DictType type = StringUtils.hasText(dictType) ? findType(dictType) : findTypeByTargetId(resource);
        if (type == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System dict not found: " + dictType);
        }
        dictType = type.getDictType();
        type.setStatus(DISABLED);
        dictTypeMapper.updateById(type);

        DictData data = new DictData();
        data.setStatus(DISABLED);
        data.setUpdateTime(LocalDateTime.now());
        dictDataMapper.update(data, new LambdaQueryWrapper<DictData>().eq(DictData::getDictType, dictType));
        return ResourceSyncResult.of(type.getId(), TARGET_TABLE, "System dict disabled: " + dictType);
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        String dictType = fieldText(resource, "dictType", false);
        DictType type = StringUtils.hasText(dictType) ? findType(dictType) : findTypeByTargetId(resource);
        if (type == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System dict not found: " + dictType);
        }
        dictType = type.getDictType();
        dictDataMapper.delete(new LambdaQueryWrapper<DictData>().eq(DictData::getDictType, dictType));
        dictTypeMapper.deleteById(type.getId());
        return ResourceSyncResult.of(type.getId(), TARGET_TABLE, "System dict deleted: " + dictType);
    }

    private void applyType(DictType type, DictPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        type.setDictName(payload.dictName());
        type.setDomainCode(payload.domainCode());
        type.setStatus(payload.status());
        type.setRemark(payload.remark());
        if (type.getCreateTime() == null) {
            type.setCreateTime(now);
        }
        type.setUpdateTime(now);
    }

    private void applyData(DictData data, DictItemPayload item) {
        LocalDateTime now = LocalDateTime.now();
        data.setDictLabel(item.label());
        data.setDictValue(item.value());
        data.setSort(item.sort());
        data.setStatus(item.status());
        data.setRemark(item.remark());
        if (data.getCreateTime() == null) {
            data.setCreateTime(now);
        }
        data.setUpdateTime(now);
    }

    private void disableRemovedItems(String dictType, Set<Long> activeItemIds, Set<String> activeItemValues) {
        List<DictData> existing = dictDataMapper.selectList(
                new LambdaQueryWrapper<DictData>().eq(DictData::getDictType, dictType));
        for (DictData data : existing) {
            boolean declaredById = data.getId() != null && activeItemIds.contains(data.getId());
            boolean declaredByValue = activeItemValues.contains(data.getDictValue());
            if (!declaredById && !declaredByValue && !Integer.valueOf(DISABLED).equals(data.getStatus())) {
                data.setStatus(DISABLED);
                dictDataMapper.updateById(data);
            }
        }
    }

    private DictType findType(String dictType) {
        return dictTypeMapper.selectOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getDictType, dictType)
                .last("limit 1"));
    }

    private DictType findTypeByTargetId(ResourceDeclaration resource) {
        Long targetId = fieldLong(resource, "targetId", false, null);
        return targetId == null ? null : dictTypeMapper.selectById(targetId);
    }

    private DictData findData(String dictType, DictItemPayload item) {
        if (item.id() != null) {
            DictData byId = dictDataMapper.selectById(item.id());
            if (byId != null) {
                return byId;
            }
        }
        return dictDataMapper.selectOne(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getDictValue, item.value())
                .last("limit 1"));
    }

    private record DictPayload(Long typeId, String dictType, String dictName, String domainCode, Integer status,
                               String remark, List<DictItemPayload> items) {

        private static DictPayload from(ResourceDeclaration resource) {
            String dictType = fieldText(resource, "dictType", true);
            return new DictPayload(
                    fieldLong(resource, "typeId", false, Long.valueOf(resource.getId())),
                    dictType,
                    fieldText(resource, "dictName", true),
                    defaultText(fieldText(resource, "domainCode", false), "COMMON"),
                    fieldInt(resource, "status", false, ENABLED),
                    fieldText(resource, "remark", false),
                    fieldList(resource, "items").stream()
                            .map(DictItemPayload::from)
                            .toList()
            );
        }
    }

    private record DictItemPayload(Long id, String label, String value, Integer sort, Integer status, String remark) {

        private static DictItemPayload from(Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalStateException("SYSTEM_DICT items must be object list");
            }
            return new DictItemPayload(
                    toLong(map.get("id"), false, null),
                    requiredText(map.get("label"), "SYSTEM_DICT item label is required"),
                    requiredText(map.get("value"), "SYSTEM_DICT item value is required"),
                    toInt(map.get("sort"), false, 0),
                    toInt(map.get("status"), false, ENABLED),
                    toText(map.get("remark"))
            );
        }
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
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("SYSTEM_DICT field is required: " + name);
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
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
