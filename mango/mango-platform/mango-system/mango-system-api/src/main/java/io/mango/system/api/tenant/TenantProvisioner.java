package io.mango.system.api.tenant;

/**
 * 租户初始化扩展点。
 * <p>
 * 各业务模块只初始化自己拥有的数据，系统模块负责在租户创建成功后统一编排。
 */
public interface TenantProvisioner {

    /**
     * 初始化指定租户的模块默认数据。
     *
     * @param context 租户初始化上下文
     */
    void provision(TenantProvisionContext context);
}
