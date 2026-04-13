package io.mango.gateway.api;

import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Public path API for Gateway to call Permission service
 *
 * @author Mango
 */
@FeignClient(name = "mango-permission", path = "/admin/public-path")
public interface SysPublicPathApi {

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
