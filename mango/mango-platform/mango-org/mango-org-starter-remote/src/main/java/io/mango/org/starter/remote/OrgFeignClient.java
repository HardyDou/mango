package io.mango.org.starter.remote;

import io.mango.common.result.R;
import io.mango.org.api.entity.SysOrg;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Org FeignClient for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "mango-org", path = "/org")
public interface OrgFeignClient {

    /**
     * Get organization tree
     *
     * @param parentId parent organization ID (0 for root)
     * @param type organization type filter (optional)
     * @return tree structure
     */
    @GetMapping("/tree")
    R<List<SysOrg>> tree(@RequestParam(required = false, defaultValue = "0") Long parentId,
                         @RequestParam(required = false) Integer type);

    /**
     * Get organization children
     *
     * @param parentId parent organization ID
     * @return children list
     */
    @GetMapping("/children/{parentId}")
    R<List<SysOrg>> children(@PathVariable Long parentId);

    /**
     * Get organization by ID
     *
     * @param id organization ID
     * @return organization
     */
    @GetMapping("/{id}")
    R<SysOrg> getById(@PathVariable Long id);
}
