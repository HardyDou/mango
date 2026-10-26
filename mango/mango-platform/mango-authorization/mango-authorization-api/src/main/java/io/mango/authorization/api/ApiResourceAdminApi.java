package io.mango.authorization.api;

import io.mango.authorization.api.query.ApiResourcePageQuery;
import io.mango.authorization.api.vo.ApiResourceFilterOptionsVO;
import io.mango.authorization.api.vo.ApiResourceVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;

/**
 * 权限标识管理契约。
 *
 * <p>面向管理后台展示接口资源登记结果，区分固定声明、Controller 扫描和人工登记来源。</p>
 */
public interface ApiResourceAdminApi {

    /**
     * 分页查询权限标识。
     *
     * @param query 分页与筛选条件
     * @return 权限标识分页结果
     */
    R<PageResult<ApiResourceVO>> pageApiResources(@Valid ApiResourcePageQuery query);

    /**
     * 查询权限标识的模块与领域筛选维度。
     *
     * @return 筛选维度
     */
    R<ApiResourceFilterOptionsVO> listApiResourceFilterOptions();
}
