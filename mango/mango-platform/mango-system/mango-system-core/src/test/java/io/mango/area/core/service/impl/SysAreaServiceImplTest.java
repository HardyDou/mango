package io.mango.area.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.area.api.entity.SysArea;
import io.mango.area.core.mapper.SysAreaMapper;
import io.mango.area.core.service.ISysAreaService;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for SysAreaServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysAreaServiceImpl Tests")
class SysAreaServiceImplTest {

    @Mock
    private SysAreaMapper areaMapper;

    private SysAreaServiceImpl sysAreaService;

    @BeforeEach
    void setUp() {
        sysAreaService = new SysAreaServiceImpl(areaMapper);
    }

    @Test
    @DisplayName("listByPid should return areas for parent")
    void listByPid_existingParent_returnsAreas() {
        SysArea area1 = createSysArea(1L, "Area 1", 0L, "1");
        SysArea area2 = createSysArea(2L, "Area 2", 0L, "1");
        when(areaMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(area1, area2));

        List<SysArea> result = sysAreaService.listByPid(0L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("listByPid should return empty list when no areas")
    void listByPid_noAreas_returnsEmptyList() {
        when(areaMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysArea> result = sysAreaService.listByPid(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getById should return area when exists")
    void getById_existingArea_returnsArea() {
        SysArea area = createSysArea(1L, "Test Area", 0L, "1");
        when(areaMapper.selectById(1L)).thenReturn(area);

        SysArea result = sysAreaService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getById should return null when area not found")
    void getById_nonExistingArea_returnsNull() {
        when(areaMapper.selectById(999L)).thenReturn(null);

        SysArea result = sysAreaService.getById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("getByAdcode should return area when exists")
    void getByAdcode_existingAdcode_returnsArea() {
        SysArea area = createSysArea(1L, "Test Area", 0L, "1");
        when(areaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(area);

        SysArea result = sysAreaService.getByAdcode(110000L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("save should return true when area is saved")
    void save_validArea_returnsTrue() {
        SysArea area = createSysArea(1L, "Test Area", 0L, "1");
        when(areaMapper.insert(area)).thenReturn(1);

        boolean result = sysAreaService.save(area);

        assertTrue(result);
    }

    @Test
    @DisplayName("update should throw exception when modifying standard area adcode")
    void update_standardAreaAdcodeChange_throwsException() {
        SysArea existing = createSysArea(1L, "Beijing", 0L, "1");
        existing.setAdcode(110000L);
        when(areaMapper.selectById(1L)).thenReturn(existing);

        SysArea updated = createSysArea(1L, "Beijing", 0L, "1");
        updated.setAdcode(110001L);

        assertThrows(UnsupportedOperationException.class, () -> sysAreaService.update(updated));
    }

    @Test
    @DisplayName("delete should throw exception when deleting standard area")
    void delete_standardArea_throwsException() {
        SysArea existing = createSysArea(1L, "Beijing", 0L, "1");
        when(areaMapper.selectById(1L)).thenReturn(existing);

        assertThrows(UnsupportedOperationException.class, () -> sysAreaService.delete(1L));
    }

    @Test
    @DisplayName("listActive should return all active areas")
    void listActive_returnsActiveAreas() {
        SysArea area = createSysArea(1L, "Active Area", 0L, "1");
        when(areaMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(area));

        List<SysArea> result = sysAreaService.listActive();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("SysAreaServiceImpl implements ISysAreaService")
    void implementsISysAreaService() {
        assertTrue(sysAreaService instanceof ISysAreaService);
    }

    private SysArea createSysArea(Long id, String name, Long pid, String areaType) {
        SysArea area = new SysArea();
        area.setId(id);
        area.setName(name);
        area.setPid(pid);
        area.setAreaType(areaType);
        area.setAdcode(110000L + id);
        area.setAreaStatus("1");
        area.setAreaSort(1);
        return area;
    }
}
