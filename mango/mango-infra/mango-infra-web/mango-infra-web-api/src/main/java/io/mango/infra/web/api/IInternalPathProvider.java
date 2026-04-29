package io.mango.infra.web.api;

import java.util.List;

/**
 * 内部路径提供器抽象。
 * <p>
 * infra-web 通过该接口获取内部路径匹配规则，
 * 避免直接耦合业务平台模块，例如 mango-authorization。
 * </p>
 * <p>
 * 具体实现由业务模块提供，例如 mango-authorization。
 * </p>
 *
 * @author Mango
 */
public interface IInternalPathProvider {

    /**
     * 获取所有内部路径匹配规则。
     *
     * @return 内部路径匹配规则列表，例如 "/admin/**"、"/internal/*"
     */
    List<String> getInternalPaths();
}
