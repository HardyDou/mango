package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.vo.SysPublicPathVO;
import io.mango.authorization.core.entity.SysPublicPath;
import io.mango.authorization.core.mapper.SysPublicPathMapper;
import io.mango.authorization.core.service.ISysPublicPathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SysPublicPathServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysPublicPathServiceImpl Tests")
class SysPublicPathServiceImplTest {

    @Mock
    private SysPublicPathMapper sysPublicPathMapper;

    private SysPublicPathServiceImpl sysPublicPathService;

    @BeforeEach
    void setUp() {
        sysPublicPathService = new SysPublicPathServiceImpl();
        // Inject mock mapper via reflection since constructor uses super()
        try {
            java.lang.reflect.Field mapperField = SysPublicPathServiceImpl.class.getSuperclass().getDeclaredField("baseMapper");
            mapperField.setAccessible(true);
            mapperField.set(sysPublicPathService, sysPublicPathMapper);
        } catch (Exception e) {
            fail("Failed to inject mock mapper: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("listEnabled should return empty list when no paths")
    void listEnabled_noPaths_returnsEmptyList() {
        when(sysPublicPathMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysPublicPathVO> result = sysPublicPathService.listEnabled();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("isPublicPath should return false for null or blank path")
    void isPublicPath_nullOrBlankPath_returnsFalse() {
        assertFalse(sysPublicPathService.isPublicPath(null));
        assertFalse(sysPublicPathService.isPublicPath(""));
        assertFalse(sysPublicPathService.isPublicPath("  "));
    }

    @Test
    @DisplayName("SysPublicPathServiceImpl implements ISysPublicPathService")
    void implementsISysPublicPathService() {
        assertTrue(sysPublicPathService instanceof ISysPublicPathService);
    }
}
