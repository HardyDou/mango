package io.mango.area.core.service;

import io.mango.area.api.entity.SysArea;

import java.util.List;
import java.util.Map;

/**
 * Area service interface (internal use only)
 *
 * @author Mango
 */
public interface ISysAreaService {

    /**
     * Get area tree.
     *
     * @param type area level (1-province, 2-city, 3-district, 4-street)
     * @return area tree structure
     */
    List<Map<String, Object>> tree(Integer type);

    /**
     * Get child areas by parent ID.
     *
     * @param parentId parent area ID
     * @return child area list
     */
    List<SysArea> listByPid(Long parentId);

    /**
     * Get area by ID.
     *
     * @param id area ID
     * @return area info
     */
    SysArea getById(Long id);

    /**
     * Get area by adcode.
     *
     * @param adcode area code
     * @return area info
     */
    SysArea getByAdcode(Long adcode);

    /**
     * Create area.
     *
     * @param area area info
     * @return success or not
     */
    boolean save(SysArea area);

    /**
     * Update area.
     *
     * @param area area info
     * @return success or not
     */
    boolean update(SysArea area);

    /**
     * Delete area.
     *
     * @param id area ID
     * @return success or not
     */
    boolean delete(Long id);

    /**
     * Get all active areas.
     *
     * @return area list
     */
    List<SysArea> listActive();
}
