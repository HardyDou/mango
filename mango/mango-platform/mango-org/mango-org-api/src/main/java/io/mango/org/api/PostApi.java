package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;

public interface PostApi {

    R<PageResult<PostVO>> page(PostPageQuery query);

    R<PostVO> get(Long id);

    R<Void> save(CreatePostCommand command);

    R<Void> update(UpdatePostCommand command);

    R<Void> delete(Long id);
}
