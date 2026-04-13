package io.mango.area.starter.remote;

import io.mango.area.api.entity.SysArea;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Area FeignClient for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "area-service", path = "/mango/area")
public interface AreaFeignClient {

    /**
     * Get area tree
     *
     * @param type area type filter (1-5)
     * @param parentId parent area ID (0 for root)
     * @return tree structure
     */
    @GetMapping("/tree")
    R<List<SysArea>> tree(@RequestParam(required = false) Integer type,
                          @RequestParam(required = false, defaultValue = "0") Long parentId);

    /**
     * Get area children by parent ID
     *
     * @param parentId parent area ID
     * @return children list
     */
    @GetMapping("/children/{parentId}")
    R<List<SysArea>> children(@PathVariable Long parentId);

    /**
     * Get area by adcode
     *
     * @param adcode area adcode
     * @return area
     */
    @GetMapping("/adcode/{adcode}")
    R<SysArea> getByAdcode(@PathVariable Long adcode);
}
