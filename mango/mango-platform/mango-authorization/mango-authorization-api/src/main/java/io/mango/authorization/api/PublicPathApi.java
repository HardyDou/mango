package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.command.PublicPathCommand;
import io.mango.authorization.api.vo.PublicPathVO;

import java.util.List;

/**
 * 公共路径配置 API 契约。
 *
 * @author Mango
 */
public interface PublicPathApi {

    /**
     * 查询全部启用的公共路径。
     *
     * @return 按类型分组的公共路径
     */
    R<List<PublicPathVO>> listEnabled();

    /**
     * 查询匿名访问路径。
     *
     * @return 匿名访问路径模式
     */
    R<List<String>> getAnonymousPaths();

    /**
     * 查询需要登录的路径。
     *
     * @return 需要登录的路径模式
     */
    R<List<String>> getLoginRequiredPaths();

    /**
     * 查询内部访问路径。
     *
     * @return 内部访问路径模式
     */
    R<List<String>> listInternalPaths();

    /**
     * 判断路径是否为公共路径。
     *
     * @param path 待检查路径
     * @return 是否公共路径
     */
    R<Boolean> isPublicPath(String path);

    R<Void> add(PublicPathCommand command);

    R<Void> update(Long id, PublicPathCommand command);

    R<Void> delete(Long id);
}
