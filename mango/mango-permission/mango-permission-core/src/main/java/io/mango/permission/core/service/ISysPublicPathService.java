package io.mango.permission.core.service;

import io.mango.permission.core.entity.SysPublicPath;
import io.mango.permission.api.vo.SysPublicPathVO;

import java.util.List;

/**
 * Public path service interface
 *
 * @author Mango
 */
public interface ISysPublicPathService {

    /**
     * Get all enabled public paths
     *
     * @return list of enabled public paths
     */
    List<SysPublicPathVO> listEnabled();

    /**
     * Get anonymous paths (type=1)
     *
     * @return anonymous path patterns
     */
    List<String> getAnonymousPaths();

    /**
     * Get paths requiring login (type=2)
     *
     * @return login-required path patterns
     */
    List<String> getLoginRequiredPaths();

    /**
     * Get internal-only paths (type=4)
     *
     * @return internal path patterns
     */
    List<String> listInternalPaths();

    /**
     * Check if path is public (anonymous or login-required)
     *
     * @param path the path to check
     * @return true if public
     */
    boolean isPublicPath(String path);

    /**
     * Add a public path
     *
     * @param publicPath the path to add
     * @return true if added
     */
    boolean addPublicPath(SysPublicPath publicPath);

    /**
     * Update a public path
     *
     * @param publicPath the path to update
     * @return true if updated
     */
    boolean updatePublicPath(SysPublicPath publicPath);

    /**
     * Delete a public path
     *
     * @param id the path id
     * @return true if deleted
     */
    boolean deletePublicPath(Long id);
}
