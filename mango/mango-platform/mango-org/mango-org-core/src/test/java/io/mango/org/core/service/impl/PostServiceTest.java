package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 岗位业务服务单元测试。
 */
class PostServiceTest {

    private PostMapper postMapper;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postMapper = mock(PostMapper.class);
        postService = new PostService();
        ReflectionTestUtils.setField(postService, "baseMapper", postMapper);
    }

    @Test
    void detailMapsCanonicalTenantAndAuditFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 16, 9, 30);
        PostEntity entity = post(10L, "研发工程师", "RD_ENGINEER");
        entity.setTenantId(2L);
        entity.setCreatedAt(createdAt);
        when(postMapper.selectById(10L)).thenReturn(entity);

        PostVO result = postService.detail(10L);

        assertThat(result.getTenantId()).isEqualTo(2L);
        assertThat(result.getCreateTime()).isEqualTo(createdAt);
    }

    @Test
    void detailRejectsMissingPost() {
        when(postMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> postService.detail(99L)).isInstanceOf(BizException.class);
    }

    @Test
    void createAppliesDefaultsAndReturnsGeneratedId() {
        CreatePostCommand command = new CreatePostCommand();
        command.setPostName("  测试工程师  ");
        command.setPostCode("  QA_ENGINEER  ");
        when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(postMapper.insert(any(PostEntity.class))).thenAnswer(invocation -> {
            PostEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });

        Long id = postService.create(command);

        assertThat(id).isEqualTo(100L);
        verify(postMapper).insert(any(PostEntity.class));
    }

    @Test
    void createRejectsDuplicateCodeBeforeInsert() {
        CreatePostCommand command = new CreatePostCommand();
        command.setPostName("测试工程师");
        command.setPostCode("QA_ENGINEER");
        when(postMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(post(20L, "已有岗位", "QA_ENGINEER")));

        assertThatThrownBy(() -> postService.create(command)).isInstanceOf(BizException.class);
    }

    @Test
    void deleteChecksExistenceAndRemovesPost() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, "研发工程师", "RD_ENGINEER"));
        when(postMapper.deleteById(10L)).thenReturn(1);
        DeleteCommand command = new DeleteCommand();
        command.setId(10L);

        assertThat(postService.delete(command)).isTrue();

        verify(postMapper).deleteById(10L);
    }

    private PostEntity post(Long id, String name, String code) {
        PostEntity entity = new PostEntity();
        entity.setId(id);
        entity.setPostName(name);
        entity.setPostCode(code);
        entity.setPostSort(1);
        entity.setPostStatus("1");
        return entity;
    }
}
