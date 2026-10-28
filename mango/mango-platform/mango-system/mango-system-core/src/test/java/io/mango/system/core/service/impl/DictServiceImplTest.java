package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTreeNodeVO;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    void createTypeDefaultsToFlatShape() {
        when(dictTypeMapper.insert((DictTypeEntity) any())).thenAnswer(invocation -> {
            DictTypeEntity entity = invocation.getArgument(0);
            entity.setId(2L);
            return 1;
        });

        assertThat(dictService.createType(typeCommand(null))).isEqualTo(2L);
        verify(dictTypeMapper).insert(argThat((DictTypeEntity entity) -> "FLAT".equals(entity.getShape())));
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
    void treeOptionsBuildNestedValues() {
        DictTypeEntity treeType = type(1L, "type1", "Type 1");
        treeType.setShape("TREE");
        DictDataEntity root = data(1L, "Root", "root");
        DictDataEntity child = data(2L, "Child", "child");
        child.setParentValue("root");
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(treeType);
        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child));

        List<DictTreeNodeVO> tree = dictService.getTreeOptions("type1");

        assertThat(tree).singleElement().satisfies(node -> {
            assertThat(node.getDictValue()).isEqualTo("root");
            assertThat(node.getChildren()).singleElement()
                    .extracting(DictTreeNodeVO::getDictValue).isEqualTo("child");
        });
    }

    @Test
    void createDataAndOptionsKeepOriginalMapping() {
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(type(1L, "type1", "Type 1"));
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

    @Test
    void groupsAreNormalizedOnWriteAndExposedOnRead() {
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(type(1L, "type1", "Type 1"));
        when(dictDataMapper.insert(any(DictDataEntity.class))).thenReturn(1);

        SaveDictDataCommand command = dataCommand();
        command.setGroups(List.of(" ORDER ", "AFTER_SALE", "ORDER", " "));
        dictService.createData(command);

        verify(dictDataMapper).insert(argThat((DictDataEntity entity) ->
                "ORDER,AFTER_SALE".equals(entity.getDictGroups())));

        DictDataEntity stored = data(1L, "Label", "value");
        stored.setDictGroups("ORDER,AFTER_SALE");
        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stored));

        assertThat(dictService.getOptions("type1")).singleElement()
                .extracting(DictOptionVO::getGroups)
                .isEqualTo(List.of("ORDER", "AFTER_SALE"));
    }

    @Test
    void groupsRejectInvalidMarkers() {
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(type(1L, "type1", "Type 1"));

        SaveDictDataCommand command = dataCommand();
        command.setGroups(List.of("ORDER GROUP"));

        assertThatThrownBy(() -> dictService.createData(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分组标记");
    }

    @Test
    void treeNodesExposeGroups() {
        DictTypeEntity treeType = type(1L, "type1", "Type 1");
        treeType.setShape("TREE");
        DictDataEntity root = data(1L, "Root", "root");
        root.setDictGroups("ORDER");
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(treeType);
        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root));

        assertThat(dictService.getTreeOptions("type1")).singleElement()
                .extracting(DictTreeNodeVO::getGroups)
                .isEqualTo(List.of("ORDER"));
    }

    @Test
    void updateDataClearsGroupsAndParentValueWhenEmpty() {
        DictDataEntity current = data(9L, "Label", "value");
        current.setDictGroups("ORDER");
        current.setParentValue("root");
        when(dictDataMapper.selectById(9L)).thenReturn(current);
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(type(1L, "type1", "Type 1"));
        when(dictDataMapper.updateById(any(DictDataEntity.class))).thenReturn(1);

        SaveDictDataCommand command = dataCommand();
        command.setId(9L);
        command.setGroups(List.of());
        command.setParentValue("  ");
        dictService.updateData(command);

        verify(dictDataMapper).updateById(argThat((DictDataEntity entity) ->
                entity.getDictGroups() == null && entity.getParentValue() == null));
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
