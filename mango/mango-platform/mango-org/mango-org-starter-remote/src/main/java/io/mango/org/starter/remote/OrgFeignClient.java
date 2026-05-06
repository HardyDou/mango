package io.mango.org.starter.remote;

import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Org FeignClient for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "mango-org", path = "/org")
public interface OrgFeignClient extends SysOrgApi {

    /**
     * Get organization tree
     *
     * @param parentId parent organization ID (0 for root)
     * @param type organization type filter (optional)
     * @return tree structure
     */
    @GetMapping("/tree")
    R<List<SysOrg>> tree(@SpringQueryMap SysOrgTreeQuery query);

    /**
     * Get organization children
     *
     * @param parentId parent organization ID
     * @return children list
     */
    @GetMapping("/children")
    @Override
    R<List<SysOrg>> children(@RequestParam Long parentId);

    /**
     * Get organization by ID
     *
     * @param id organization ID
     * @return organization
     */
    @GetMapping("/detail")
    @Override
    R<SysOrg> getById(@RequestParam Long id);
}
