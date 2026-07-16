package io.mango.org.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.PostApi;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 岗位管理远程适配器。
 */
@FeignClient(name = "mango-org", contextId = "postFeignClient", path = "/post")
public interface PostFeignClient extends PostApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PostVO>> page(@SpringQueryMap PostPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PostVO> get(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> save(@RequestBody CreatePostCommand command);

    @Override
    @PutMapping
    R<Boolean> update(@RequestBody UpdatePostCommand command);

    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);
}
