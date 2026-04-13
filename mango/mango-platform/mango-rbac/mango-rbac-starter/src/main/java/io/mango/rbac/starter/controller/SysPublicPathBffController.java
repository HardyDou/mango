package io.mango.rbac.starter.controller;

import io.mango.common.result.R;
import io.mango.rbac.core.entity.SysPublicPath;
import io.mango.rbac.api.vo.SysPublicPathVO;
import io.mango.rbac.core.service.ISysPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public path BFF controller for frontend management
 *
 * @author Mango
 */
@RestController
@RequestMapping("/bff/permission/public-path")
@RequiredArgsConstructor
public class SysPublicPathBffController {

    private final ISysPublicPathService publicPathService;

    @GetMapping
    public R<?> list() {
        return R.ok(publicPathService.listEnabled());
    }

    @PostMapping
    public R<Void> add(@RequestBody SysPublicPathVO vo) {
        SysPublicPath entity = toEntity(vo);
        boolean success = publicPathService.addPublicPath(entity);
        return success ? R.ok() : R.fail("添加失败");
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysPublicPathVO vo) {
        SysPublicPath entity = toEntity(vo);
        entity.setId(id);
        boolean success = publicPathService.updatePublicPath(entity);
        return success ? R.ok() : R.fail("更新失败");
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean success = publicPathService.deletePublicPath(id);
        return success ? R.ok() : R.fail("删除失败");
    }

    private SysPublicPath toEntity(SysPublicPathVO vo) {
        SysPublicPath entity = new SysPublicPath();
        entity.setPath(vo.getPath());
        entity.setPathType(vo.getPathType());
        entity.setDescription(vo.getDescription());
        entity.setPriority(vo.getPriority());
        entity.setStatus(vo.getStatus());
        return entity;
    }
}
