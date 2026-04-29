package io.mango.authorization.starter.controller;

import io.mango.authorization.api.PublicPathApi;
import io.mango.authorization.api.command.PublicPathCommand;
import io.mango.common.result.R;
import io.mango.authorization.core.entity.PublicPath;
import io.mango.authorization.api.vo.PublicPathVO;
import io.mango.authorization.core.service.IPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公共路径管理控制器。
 *
 * @author Mango
 */
@RestController
@RequestMapping("/authorization/admin/public-path")
@RequiredArgsConstructor
@Validated
public class PublicPathController implements PublicPathApi {

    private final IPublicPathService publicPathService;

    // ==================== 内部接口 ====================

    /**
     * 查询全部启用的公共路径，供其他微服务使用。
     */
    @GetMapping("/list")
    @Override
    public R<List<PublicPathVO>> listEnabled() {
        return R.ok(publicPathService.listEnabled());
    }

    /**
     * 查询匿名访问路径。
     */
    @GetMapping("/anonymous")
    @Override
    public R<List<String>> getAnonymousPaths() {
        return R.ok(publicPathService.getAnonymousPaths());
    }

    /**
     * 查询需要登录的路径。
     */
    @GetMapping("/login-required")
    @Override
    public R<List<String>> getLoginRequiredPaths() {
        return R.ok(publicPathService.getLoginRequiredPaths());
    }

    /**
     * 查询内部访问路径。
     */
    @GetMapping("/internal")
    @Override
    public R<List<String>> listInternalPaths() {
        return R.ok(publicPathService.listInternalPaths());
    }

    /**
     * 判断路径是否为公共路径。
     */
    @GetMapping("/check")
    @Override
    public R<Boolean> isPublicPath(@RequestParam String path) {
        return R.ok(publicPathService.isPublicPath(path));
    }

    // ==================== 管理接口 ====================

    /**
     * 新增公共路径。
     */
    @PostMapping
    @Override
    public R<Void> add(@Validated @RequestBody PublicPathCommand command) {
        boolean success = publicPathService.addPublicPath(toEntity(command));
        return success ? R.ok() : R.fail("添加失败");
    }

    /**
     * 更新公共路径。
     */
    @PutMapping("/{id}")
    @Override
    public R<Void> update(
            @PathVariable Long id,
            @Validated @RequestBody PublicPathCommand command) {
        PublicPath publicPath = toEntity(command);
        publicPath.setId(id);
        boolean success = publicPathService.updatePublicPath(publicPath);
        return success ? R.ok() : R.fail("更新失败");
    }

    /**
     * 删除公共路径。
     */
    @DeleteMapping("/{id}")
    @Override
    public R<Void> delete(@PathVariable Long id) {
        boolean success = publicPathService.deletePublicPath(id);
        return success ? R.ok() : R.fail("删除失败");
    }

    private PublicPath toEntity(PublicPathCommand command) {
        PublicPath publicPath = new PublicPath();
        publicPath.setId(command.getId());
        publicPath.setPath(command.getPath());
        publicPath.setPathType(command.getPathType());
        publicPath.setDescription(command.getDescription());
        publicPath.setPriority(command.getPriority());
        publicPath.setStatus(command.getStatus());
        return publicPath;
    }
}
