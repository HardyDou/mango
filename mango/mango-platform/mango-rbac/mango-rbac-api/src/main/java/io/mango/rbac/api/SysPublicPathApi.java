package io.mango.rbac.api;

import io.mango.common.result.R;
import io.mango.rbac.api.vo.SysPublicPathVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public path configuration API
 *
 * @author Mango
 */
@FeignClient(name = "mango-rbac", path = "/admin/public-path")
public interface SysPublicPathApi {

    /**
     * Get all enabled public paths
     *
     * @return public paths grouped by type
     */
    @GetMapping("/list")
    R<List<SysPublicPathVO>> listEnabled();

    /**
     * Get anonymous paths (type=1)
     *
     * @return anonymous path patterns
     */
    @GetMapping("/anonymous")
    R<List<String>> getAnonymousPaths();

    /**
     * Get paths requiring login (type=2)
     *
     * @return login-required path patterns
     */
    @GetMapping("/login-required")
    R<List<String>> getLoginRequiredPaths();

    /**
     * Get internal-only paths (type=4)
     *
     * @return internal path patterns
     */
    @GetMapping("/internal")
    R<List<String>> listInternalPaths();

    /**
     * Check if path is public
     *
     * @param path the path to check
     * @return true if public (anonymous or login-required)
     */
    @GetMapping("/check")
    R<Boolean> isPublicPath(@RequestParam String path);
}
