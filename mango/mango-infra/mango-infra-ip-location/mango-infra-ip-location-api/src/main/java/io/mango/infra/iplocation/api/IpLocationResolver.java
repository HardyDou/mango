package io.mango.infra.iplocation.api;

/**
 * IP 归属地解析器。
 */
public interface IpLocationResolver {

    /**
     * 解析 IP 归属地。实现必须兜底异常，不能阻断主业务流程。
     *
     * @param ip IPv4/IPv6 地址
     * @return 解析结果，无法解析时返回 unresolved 结果
     */
    IpLocation resolve(String ip);
}
