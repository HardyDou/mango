package io.mango.authorization.api.vo;


/**
 * API 资源注册结果。
 *
 * @param scanned 扫描到的资源数量
 * @param created 新增资源数量
 * @param updated 更新资源数量
 *
 * @author hardy
 */
public record ApiResourceRegisterResultVO(
        int scanned,
        int created,
        int updated) {

    public static ApiResourceRegisterResultVO empty() {
        return new ApiResourceRegisterResultVO(0, 0, 0);
    }
}
