package io.mango.org.core.service;

import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.entity.PostEntity;

/**
 * 岗位业务服务。
 */
public interface IPostService extends MangoTypedCrudService<
        PostEntity, CreatePostCommand, UpdatePostCommand, PostPageQuery, PostVO, Long> {

    PageResult<PostVO> pageResult(PostPageQuery query);
}
