package io.mango.authorization.starter.controller;

import io.mango.common.result.R;
import io.mango.authorization.core.entity.PublicPath;
import io.mango.authorization.api.vo.PublicPathVO;
import io.mango.authorization.core.service.IPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 面向前端管理的公共路径 BFF 控制器。
 *
 * @author Mango
 */
@RestController
@RequestMapping("/authorization/bff/permission/public-path")
@RequiredArgsConstructor
public class PublicPathBffController {

    private final IPublicPathService publicPathService;

    @GetMapping
    public R<?> list() {
        return R.ok(publicPathService.listEnabled());
    }

    @PostMapping
    public R<Void> add(@RequestBody PublicPathVO vo) {
        PublicPath entity = toEntity(vo);
        boolean success = publicPathService.addPublicPath(entity);
        return success ? R.ok() : R.fail("添加失败");
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody PublicPathVO vo) {
        PublicPath entity = toEntity(vo);
        entity.setId(id);
        boolean success = publicPathService.updatePublicPath(entity);
        return success ? R.ok() : R.fail("更新失败");
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean success = publicPathService.deletePublicPath(id);
        return success ? R.ok() : R.fail("删除失败");
    }

    private PublicPath toEntity(PublicPathVO vo) {
        PublicPath entity = new PublicPath();
        entity.setPath(vo.getPath());
        entity.setPathType(vo.getPathType());
        entity.setDescription(vo.getDescription());
        entity.setPriority(vo.getPriority());
        entity.setStatus(vo.getStatus());
        return entity;
    }
}
