package io.mango.org.core.service;

import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;

public interface IPostService {

    PostVO getById(Long id);

    PageResult<PostVO> page(PostPageQuery query);

    void save(CreatePostCommand command);

    void update(UpdatePostCommand command);

    void delete(Long id);
}
