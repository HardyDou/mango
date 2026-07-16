package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.core.entity.DictDataEntity;
import io.mango.system.core.entity.DictTypeEntity;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
import io.mango.system.core.service.IDictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictServiceImplTest {

    @Mock
    private DictTypeMapper dictTypeMapper;
    @Mock
    private DictDataMapper dictDataMapper;

    private DictService dictService;

    @BeforeEach
    void setUp() {
        dictService = new DictService(dictTypeMapper, dictDataMapper);
    }

    @Test
    void listTypesReturnsMappedValues() {
        when(dictTypeMapper.selectList(any())).thenReturn(List.of(
                type(1L, "type1", "Type 1"), type(2L, "type2", "Type 2")));

        assertThat(dictService.listTypes(null)).hasSize(2);
    }

    @Test
    void getTypeReturnsValueAndRejectsMissingType() {
        when(dictTypeMapper.selectById(1L)).thenReturn(type(1L, "type1", "Type 1"));
        when(dictTypeMapper.selectById(999L)).thenReturn(null);

        assertThat(dictService.getType(1L).getDictType()).isEqualTo("type1");
        assertThatThrownBy(() -> dictService.getType(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字典类型不存在");
    }

    @Test
    void createTypeMapsCommandAndInsertsEntity() {
        when(dictTypeMapper.insert(any(DictTypeEntity.class))).thenReturn(1);

        dictService.createType(typeCommand(null));

        verify(dictTypeMapper).insert(any(DictTypeEntity.class));
    }

    @Test
    void updateTypeRejectsMissingId() {
        assertThatThrownBy(() -> dictService.updateType(typeCommand(null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ID不能为空");
    }

    @Test
    void deleteTypeDeletesOnlyWhenUnused() {
        when(dictTypeMapper.selectById(1L)).thenReturn(type(1L, "type1", "Type 1"));
        when(dictDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dictTypeMapper.deleteById(1L)).thenReturn(1);

        assertThat(dictService.deleteType(1L)).isTrue();
    }

    @Test
    void createDataAndOptionsKeepOriginalMapping() {
        when(dictDataMapper.insert(any(DictDataEntity.class))).thenReturn(1);
        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(data(1L, "Label", "value")));

        dictService.createData(dataCommand());
        List<DictOptionVO> options = dictService.getOptions("type1");

        verify(dictDataMapper).insert(any(DictDataEntity.class));
        assertThat(options).singleElement()
                .satisfies(option -> {
                    assertThat(option.getLabel()).isEqualTo("Label");
                    assertThat(option.getValue()).isEqualTo("value");
                });
        assertThat(dictService).isInstanceOf(IDictService.class);
    }

    private SaveDictTypeCommand typeCommand(Long id) {
        SaveDictTypeCommand command = new SaveDictTypeCommand();
        command.setId(id);
        command.setDictType("new_type");
        command.setDictName("New Type");
        return command;
    }

    private SaveDictDataCommand dataCommand() {
        SaveDictDataCommand command = new SaveDictDataCommand();
        command.setDictType("type1");
        command.setDictLabel("Label");
        command.setDictValue("value");
        return command;
    }

    private DictTypeEntity type(Long id, String dictType, String dictName) {
        DictTypeEntity entity = new DictTypeEntity();
        entity.setId(id);
        entity.setDictType(dictType);
        entity.setDictName(dictName);
        entity.setDomainCode("COMMON");
        entity.setStatus(1);
        return entity;
    }

    private DictDataEntity data(Long id, String label, String value) {
        DictDataEntity entity = new DictDataEntity();
        entity.setId(id);
        entity.setDictType("type1");
        entity.setDictLabel(label);
        entity.setDictValue(value);
        entity.setSort(1);
        entity.setStatus(1);
        return entity;
    }
}
