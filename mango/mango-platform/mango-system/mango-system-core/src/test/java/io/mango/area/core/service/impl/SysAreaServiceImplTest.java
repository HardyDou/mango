package io.mango.area.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.area.api.command.SaveAreaCommand;
import io.mango.area.api.vo.SysAreaVO;
import io.mango.area.core.entity.SysAreaEntity;
import io.mango.area.core.mapper.SysAreaMapper;
import io.mango.area.core.service.ISysAreaService;
import io.mango.common.exception.BizException;
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
class SysAreaServiceImplTest {

    @Mock
    private SysAreaMapper areaMapper;

    private SysAreaService areaService;

    @BeforeEach
    void setUp() {
        areaService = new SysAreaService(areaMapper);
    }

    @Test
    void listByPidReturnsMappedAreas() {
        when(areaMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(area(1L, "Area 1", 0L, "1"), area(2L, "Area 2", 0L, "1")));

        List<SysAreaVO> result = areaService.listByPid(0L);

        assertThat(result).extracting(SysAreaVO::getName).containsExactly("Area 1", "Area 2");
    }

    @Test
    void getByIdReturnsMappedArea() {
        when(areaMapper.selectById(1L)).thenReturn(area(1L, "Test Area", 0L, "1"));

        assertThat(areaService.getById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getByIdRejectsMissingAreaWithModuleCode() {
        when(areaMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> areaService.getById(999L))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(404);
    }

    @Test
    void createMapsCommandAndInsertsEntity() {
        when(areaMapper.insert(any(SysAreaEntity.class))).thenReturn(1);

        areaService.create(command(null, "Custom Area", 0L, "5", 990001L));

        verify(areaMapper).insert(any(SysAreaEntity.class));
    }

    @Test
    void updateRejectsStandardAreaAdcodeChange() {
        SysAreaEntity existing = area(1L, "Beijing", 0L, "1");
        existing.setAdcode(110000L);
        when(areaMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> areaService.update(command(1L, "Beijing", 0L, "1", 110001L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Standard administrative area adcode cannot be modified");
    }

    @Test
    void deleteRejectsStandardArea() {
        when(areaMapper.selectById(1L)).thenReturn(area(1L, "Beijing", 0L, "1"));

        assertThatThrownBy(() -> areaService.delete(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Standard administrative area cannot be deleted");
    }

    @Test
    void listActiveReturnsMappedAreas() {
        when(areaMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(area(1L, "Active Area", 0L, "1")));

        assertThat(areaService.listActive()).extracting(SysAreaVO::getName).containsExactly("Active Area");
        assertThat(areaService).isInstanceOf(ISysAreaService.class);
    }

    private SysAreaEntity area(Long id, String name, Long pid, String areaType) {
        SysAreaEntity area = new SysAreaEntity();
        area.setId(id);
        area.setName(name);
        area.setPid(pid);
        area.setAreaType(areaType);
        area.setAdcode(110000L + id);
        area.setAreaStatus("1");
        area.setAreaSort(1);
        return area;
    }

    private SaveAreaCommand command(Long id, String name, Long pid, String areaType, Long adcode) {
        SaveAreaCommand command = new SaveAreaCommand();
        command.setId(id);
        command.setName(name);
        command.setPid(pid);
        command.setAreaType(areaType);
        command.setAdcode(adcode);
        command.setAreaStatus("1");
        command.setAreaSort(1);
        return command;
    }
}
