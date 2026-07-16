package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

/**
 * 岗位管理 API 契约。
 */
@Validated
public interface PostApi {

    R<PageResult<PostVO>> page(@Valid PostPageQuery query);

    R<PostVO> get(
            @NotNull(message = "岗位ID不能为空")
            @Positive(message = "岗位ID必须大于0") Long id);

    R<Long> save(@Valid CreatePostCommand command);

    R<Boolean> update(@Valid UpdatePostCommand command);

    R<Boolean> delete(
            @NotNull(message = "岗位ID不能为空")
            @Positive(message = "岗位ID必须大于0") Long id);
}
