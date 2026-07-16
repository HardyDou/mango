package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.enums.PostCode;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.service.IPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 岗位业务服务实现。
 */
@Service
public class PostService extends MangoCrudServiceImpl<PostMapper, PostEntity>
        implements IPostService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreatePostCommand command) {
        validateCreate(command);
        Object id = createByCommand(command);
        Require.isTrue(id instanceof Long, PostCode.VALIDATION_ERROR, "岗位ID生成失败");
        return (Long) id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdatePostCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "岗位修改命令不能为空");
        Require.notNull(command.getId(), PostCode.POST_ID_REQUIRED);
        return updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "岗位删除命令不能为空");
        Require.notNull(command.getId(), PostCode.POST_ID_REQUIRED);
        return deleteById(command.getId());
    }

    @Override
    public PersistencePageResult<PostVO> page(PostPageQuery query) {
        PostPageQuery resolved = query == null ? new PostPageQuery() : query;
        PersistencePageResult<?> source = pageByQuery(resolved);
        List<PostVO> records = source.getRecords().stream().map(PostVO.class::cast).toList();
        return PersistencePageResult.of(records, source.getTotal(), source.getPage(), source.getSize());
    }

    @Override
    public PageResult<PostVO> pageResult(PostPageQuery query) {
        PersistencePageResult<PostVO> result = page(query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getPage(), result.getSize());
    }

    @Override
    public PostVO detail(Long id) {
        Require.notNull(id, PostCode.POST_ID_REQUIRED);
        PostEntity entity = getById(id);
        Require.notNull(entity, PostCode.POST_NOT_FOUND);
        return toVO(entity);
    }

    @Override
    protected Class<PostEntity> entityType() {
        return PostEntity.class;
    }

    @Override
    protected QueryWrapper<PostEntity> buildQueryWrapper(Object queryObject) {
        PostPageQuery query = (PostPageQuery) queryObject;
        QueryWrapper<PostEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .like(StringUtils.hasText(query.getPostName()), PostEntity::getPostName, query.getPostName())
                .like(StringUtils.hasText(query.getPostCode()), PostEntity::getPostCode, query.getPostCode())
                .eq(StringUtils.hasText(query.getPostStatus()), PostEntity::getPostStatus, query.getPostStatus())
                .orderByAsc(PostEntity::getPostSort)
                .orderByDesc(PostEntity::getCreatedAt);
        return wrapper;
    }

    @Override
    protected void beforeCreate(Object commandObject, PostEntity entity) {
        CreatePostCommand command = (CreatePostCommand) commandObject;
        Require.notBlank(command.getPostName(), PostCode.VALIDATION_ERROR, "岗位名称不能为空");
        Require.notBlank(command.getPostCode(), PostCode.VALIDATION_ERROR, "岗位编码不能为空");
        Require.isTrue(findByCode(command.getPostCode(), null) == null, PostCode.POST_CODE_EXISTS);
        entity.setPostName(command.getPostName().trim());
        entity.setPostCode(command.getPostCode().trim());
        entity.setPostSort(command.getPostSort() == null ? 0 : command.getPostSort());
        entity.setPostStatus(StringUtils.hasText(command.getPostStatus()) ? command.getPostStatus() : "1");
        entity.setRemark(command.getRemark());
    }

    @Override
    protected void beforeUpdate(Object commandObject, PostEntity entity) {
        UpdatePostCommand command = (UpdatePostCommand) commandObject;
        PostEntity current = getById(command.getId());
        Require.notNull(current, PostCode.POST_NOT_FOUND);
        Require.notBlank(command.getPostName(), PostCode.VALIDATION_ERROR, "岗位名称不能为空");
        Require.notBlank(command.getPostCode(), PostCode.VALIDATION_ERROR, "岗位编码不能为空");
        Require.isTrue(findByCode(command.getPostCode(), command.getId()) == null, PostCode.POST_CODE_EXISTS);
        entity.setPostName(command.getPostName().trim());
        entity.setPostCode(command.getPostCode().trim());
        entity.setPostSort(command.getPostSort() == null ? 0 : command.getPostSort());
        entity.setPostStatus(StringUtils.hasText(command.getPostStatus()) ? command.getPostStatus() : "1");
        entity.setRemark(command.getRemark());
    }

    @Override
    protected void beforeDelete(Object id) {
        Require.notNull(getById((Long) id), PostCode.POST_NOT_FOUND);
    }

    @Override
    protected PostVO toVO(PostEntity entity) {
        if (entity == null) {
            return null;
        }
        PostVO vo = new PostVO();
        vo.setId(entity.getId());
        vo.setPostName(entity.getPostName());
        vo.setPostCode(entity.getPostCode());
        vo.setPostSort(entity.getPostSort());
        vo.setPostStatus(entity.getPostStatus());
        vo.setRemark(entity.getRemark());
        vo.setTenantId(entity.getTenantIdAsLong());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private void validateCreate(CreatePostCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "岗位新增命令不能为空");
        Require.notBlank(command.getPostName(), PostCode.VALIDATION_ERROR, "岗位名称不能为空");
        Require.notBlank(command.getPostCode(), PostCode.VALIDATION_ERROR, "岗位编码不能为空");
    }

    private PostEntity findByCode(String postCode, Long excludedId) {
        LambdaQueryWrapper<PostEntity> wrapper = new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getPostCode, postCode.trim());
        wrapper.ne(excludedId != null, PostEntity::getId, excludedId);
        wrapper.last("LIMIT 1");
        return list(wrapper).stream().findFirst().orElse(null);
    }
}
