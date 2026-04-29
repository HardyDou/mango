package io.mango.authorization.starter.remote;

import io.mango.authorization.api.PublicPathApi;
import io.mango.authorization.api.command.PublicPathCommand;
import io.mango.authorization.api.vo.PublicPathVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 公共路径远程客户端。
 *
 * @author Mango
 */
@FeignClient(name = "mango-authorization", path = "/authorization/admin/public-path")
public interface PublicPathFeignClient extends PublicPathApi {

    @Override
    @GetMapping("/list")
    R<List<PublicPathVO>> listEnabled();

    @Override
    @GetMapping("/anonymous")
    R<List<String>> getAnonymousPaths();

    @Override
    @GetMapping("/login-required")
    R<List<String>> getLoginRequiredPaths();

    @Override
    @GetMapping("/internal")
    R<List<String>> listInternalPaths();

    @Override
    @GetMapping("/check")
    R<Boolean> isPublicPath(@RequestParam String path);

    @Override
    @PostMapping
    R<Void> add(@RequestBody PublicPathCommand command);

    @Override
    @PutMapping("/{id}")
    R<Void> update(@PathVariable Long id, @RequestBody PublicPathCommand command);

    @Override
    @DeleteMapping("/{id}")
    R<Void> delete(@PathVariable Long id);
}
