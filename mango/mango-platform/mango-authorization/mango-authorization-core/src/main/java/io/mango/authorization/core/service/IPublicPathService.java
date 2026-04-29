package io.mango.authorization.core.service;

import io.mango.authorization.core.entity.PublicPath;
import io.mango.authorization.api.vo.PublicPathVO;

import java.util.List;

/**
 * 公共路径内部服务接口。
 *
 * @author Mango
 */
public interface IPublicPathService {

    /**
     * 查询全部启用的公共路径。
     *
     * @return 启用公共路径列表
     */
    List<PublicPathVO> listEnabled();

    /**
     * 查询匿名访问路径。
     *
     * @return 匿名访问路径模式
     */
    List<String> getAnonymousPaths();

    /**
     * 查询需要登录的路径。
     *
     * @return 需要登录的路径模式
     */
    List<String> getLoginRequiredPaths();

    /**
     * 查询内部访问路径。
     *
     * @return 内部访问路径模式
     */
    List<String> listInternalPaths();

    /**
     * 判断路径是否公共路径。
     *
     * @param path 待检查路径
     * @return 是否公共路径
     */
    boolean isPublicPath(String path);

    /**
     * 新增公共路径。
     *
     * @param publicPath 待新增路径
     * @return 是否新增成功
     */
    boolean addPublicPath(PublicPath publicPath);

    /**
     * 更新公共路径。
     *
     * @param publicPath 待更新路径
     * @return 是否更新成功
     */
    boolean updatePublicPath(PublicPath publicPath);

    /**
     * 删除公共路径。
     *
     * @param id 公共路径 ID
     * @return 是否删除成功
     */
    boolean deletePublicPath(Long id);
}
