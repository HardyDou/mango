package io.mango.permission.starter.controller;

import io.mango.common.result.R;
import io.mango.permission.core.entity.SysPublicPath;
import io.mango.permission.api.vo.SysPublicPathVO;
import io.mango.permission.core.service.ISysPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public path management controller
 *
 * @author Mango
 */
@RestController
@RequestMapping("/admin/public-path")
@RequiredArgsConstructor
@Validated
public class SysPublicPathController {

    private final ISysPublicPathService publicPathService;

    // ==================== Internal API ====================

    /**
     * Get all enabled public paths (for other microservices)
     */
    @GetMapping("/list")
    public R<List<SysPublicPathVO>> listEnabled() {
        return R.ok(publicPathService.listEnabled());
    }

    /**
     * Get anonymous paths (type=1)
     */
    @GetMapping("/anonymous")
    public R<List<String>> getAnonymousPaths() {
        return R.ok(publicPathService.getAnonymousPaths());
    }

    /**
     * Get login-required paths (type=2)
     */
    @GetMapping("/login-required")
    public R<List<String>> getLoginRequiredPaths() {
        return R.ok(publicPathService.getLoginRequiredPaths());
    }

    /**
     * Get internal-only paths (type=4)
     */
    @GetMapping("/internal")
    public R<List<String>> listInternalPaths() {
        return R.ok(publicPathService.listInternalPaths());
    }

    /**
     * Check if path is public
     */
    @GetMapping("/check")
    public R<Boolean> isPublicPath(@RequestParam String path) {
        return R.ok(publicPathService.isPublicPath(path));
    }

    // ==================== Admin Management API ====================

    /**
     * Add a new public path
     */
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysPublicPath publicPath) {
        boolean success = publicPathService.addPublicPath(publicPath);
        return success ? R.ok() : R.fail("添加失败");
    }

    /**
     * Update a public path
     */
    @PutMapping("/{id}")
    public R<Void> update(
            @PathVariable Long id,
            @Validated @RequestBody SysPublicPath publicPath) {
        publicPath.setId(id);
        boolean success = publicPathService.updatePublicPath(publicPath);
        return success ? R.ok() : R.fail("更新失败");
    }

    /**
     * Delete a public path
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean success = publicPathService.deletePublicPath(id);
        return success ? R.ok() : R.fail("删除失败");
    }
}
