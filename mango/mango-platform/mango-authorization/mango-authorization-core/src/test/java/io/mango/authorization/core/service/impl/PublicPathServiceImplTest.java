package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.vo.PublicPathVO;
import io.mango.authorization.core.entity.PublicPath;
import io.mango.authorization.core.mapper.PublicPathMapper;
import io.mango.authorization.core.service.IPublicPathService;
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
 * PublicPathServiceImpl 单元测试。
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicPathServiceImpl 测试")
class PublicPathServiceImplTest {

    @Mock
    private PublicPathMapper publicPathMapper;

    private PublicPathServiceImpl publicPathService;

    @BeforeEach
    void setUp() {
        publicPathService = new PublicPathServiceImpl();
        // 构造函数使用父类逻辑，这里通过反射注入 mock mapper。
        try {
            java.lang.reflect.Field mapperField = PublicPathServiceImpl.class.getSuperclass().getDeclaredField("baseMapper");
            mapperField.setAccessible(true);
            mapperField.set(publicPathService, publicPathMapper);
        } catch (Exception e) {
            fail("Failed to inject mock mapper: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("listEnabled should return empty list when no paths")
    void listEnabled_noPaths_returnsEmptyList() {
        when(publicPathMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<PublicPathVO> result = publicPathService.listEnabled();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("isPublicPath should return false for null or blank path")
    void isPublicPath_nullOrBlankPath_returnsFalse() {
        assertFalse(publicPathService.isPublicPath(null));
        assertFalse(publicPathService.isPublicPath(""));
        assertFalse(publicPathService.isPublicPath("  "));
    }

    @Test
    @DisplayName("PublicPathServiceImpl implements IPublicPathService")
    void implementsIPublicPathService() {
        assertTrue(publicPathService instanceof IPublicPathService);
    }
}
