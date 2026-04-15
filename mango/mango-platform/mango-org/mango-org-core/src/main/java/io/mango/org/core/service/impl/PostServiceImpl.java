package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private final PostMapper postMapper;

    @Override
    public PostVO getById(Long id) {
        PostEntity entity = postMapper.selectById(id);
        Require.notNull(entity, 404, "记录不存在");
        return toVO(entity);
    }

    @Override
    public PageResult<PostVO> page(PostPageQuery query) {
        LambdaQueryWrapper<PostEntity> wrapper = new LambdaQueryWrapper<>();
        IPage<PostEntity> page = postMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void save(CreatePostCommand command) {
        postMapper.insert(toEntity(command));
    }

    @Override
    public void update(UpdatePostCommand command) {
        Require.notNull(command.getId(), 400, "ID 不能为空");
        postMapper.updateById(toEntity(command));
    }

    @Override
    public void delete(Long id) {
        postMapper.deleteById(id);
    }

    private PostVO toVO(PostEntity entity) {
        if (entity == null) {
            return null;
        }
        PostVO vo = new PostVO();
        vo.setId(entity.getId());
        return vo;
    }

    private PostEntity toEntity(CreatePostCommand command) {
        if (command == null) {
            return null;
        }
        PostEntity entity = new PostEntity();
        return entity;
    }

    private PostEntity toEntity(UpdatePostCommand command) {
        if (command == null) {
            return null;
        }
        PostEntity entity = new PostEntity();
        entity.setId(command.getId());
        return entity;
    }
}
