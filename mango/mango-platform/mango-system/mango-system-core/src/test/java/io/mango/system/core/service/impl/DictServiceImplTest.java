package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.system.api.po.DictTypePo;
import io.mango.system.api.po.DictDataPo;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.core.entity.DictType;
import io.mango.system.core.entity.DictData;
import io.mango.system.core.mapper.DictTypeMapper;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.service.IDictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DictServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictServiceImpl Tests")
class DictServiceImplTest {

    @Mock
    private DictTypeMapper dictTypeMapper;

    @Mock
    private DictDataMapper dictDataMapper;

    private DictServiceImpl dictService;

    @BeforeEach
    void setUp() {
        dictService = new DictServiceImpl(dictTypeMapper, dictDataMapper);
    }

    @Test
    @DisplayName("listTypes should return all types")
    void listTypes_returnsAllTypes() {
        DictType type1 = createDictType(1L, "type1", "Type 1");
        DictType type2 = createDictType(2L, "type2", "Type 2");
        when(dictTypeMapper.selectList(any())).thenReturn(Arrays.asList(type1, type2));

        R<List<DictTypeVO>> result = dictService.listTypes();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("getType should return type when exists")
    void getType_existingType_returnsType() {
        DictType type = createDictType(1L, "type1", "Type 1");
        when(dictTypeMapper.selectById(1L)).thenReturn(type);

        R<DictTypeVO> result = dictService.getType(1L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("getType should fail when type not found")
    void getType_nonExistingType_returnsFail() {
        when(dictTypeMapper.selectById(999L)).thenReturn(null);

        R<DictTypeVO> result = dictService.getType(999L);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("createType should insert and return id")
    void createType_validPo_insertsAndReturnsId() {
        DictTypePo po = new DictTypePo();
        po.setDictType("new_type");
        po.setDictName("New Type");
        when(dictTypeMapper.insert(any(DictType.class))).thenReturn(1);

        R<Long> result = dictService.createType(po);

        assertTrue(result.isSuccess());
        verify(dictTypeMapper).insert(any(DictType.class));
    }

    @Test
    @DisplayName("updateType should fail when id is null")
    void updateType_nullId_returnsFail() {
        DictTypePo po = new DictTypePo();
        po.setId(null);

        R<Boolean> result = dictService.updateType(po);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("deleteType should delete type")
    void deleteType_existingType_deletesType() {
        DictType type = createDictType(1L, "type1", "Type 1");
        when(dictTypeMapper.selectById(1L)).thenReturn(type);
        when(dictDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dictTypeMapper.deleteById(1L)).thenReturn(1);

        R<Boolean> result = dictService.deleteType(1L);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("getData should return data when exists")
    void getData_existingData_returnsData() {
        DictData data = createDictData(1L, "label", "value");
        when(dictDataMapper.selectById(1L)).thenReturn(data);

        R<DictDataVO> result = dictService.getData(1L);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("createData should insert and return id")
    void createData_validPo_insertsAndReturnsId() {
        DictDataPo po = new DictDataPo();
        po.setDictType("type1");
        po.setDictLabel("Label");
        po.setDictValue("value");
        when(dictDataMapper.insert(any(DictData.class))).thenReturn(1);

        R<Long> result = dictService.createData(po);

        assertTrue(result.isSuccess());
        verify(dictDataMapper).insert(any(DictData.class));
    }

    @Test
    @DisplayName("getOptions should return options for type")
    void getOptions_existingType_returnsOptions() {
        DictData data = createDictData(1L, "Label", "value");
        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(data));

        R<List<DictOptionVO>> result = dictService.getOptions("type1");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Label", result.getData().get(0).getLabel());
    }

    @Test
    @DisplayName("DictServiceImpl implements IDictService")
    void implementsIDictService() {
        assertTrue(dictService instanceof IDictService);
    }

    private DictType createDictType(Long id, String dictType, String dictName) {
        DictType type = new DictType();
        type.setId(id);
        type.setDictType(dictType);
        type.setDictName(dictName);
        type.setStatus(1);
        return type;
    }

    private DictData createDictData(Long id, String dictLabel, String dictValue) {
        DictData data = new DictData();
        data.setId(id);
        data.setDictType("type1");
        data.setDictLabel(dictLabel);
        data.setDictValue(dictValue);
        data.setSort(1);
        data.setStatus(1);
        return data;
    }
}
