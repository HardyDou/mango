package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PostApi {

    R<PageResult<PostVO>> page(PostPageQuery query);

    R<PostVO> get(@PathVariable Long id);

    R<Void> save(@RequestBody CreatePostCommand command);

    R<Void> update(@PathVariable Long id, @RequestBody UpdatePostCommand command);

    R<Void> delete(@PathVariable Long id);
}
