package io.mango.org.starter.controller;

import io.mango.org.api.PostApi;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController implements PostApi {

    private final IPostService postService;

    @Override
    @GetMapping("/page")
    public R<PageResult<PostVO>> page(PostPageQuery query) {
        return R.ok(postService.page(query));
    }

    @Override
    @GetMapping("/{id}")
    public R<PostVO> get(@PathVariable Long id) {
        return R.ok(postService.getById(id));
    }

    @Override
    @PostMapping
    public R<Void> save(@RequestBody CreatePostCommand command) {
        postService.save(command);
        return R.ok();
    }

    @Override
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody UpdatePostCommand command) {
        command.setId(id);
        postService.update(command);
        return R.ok();
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return R.ok();
    }
}
