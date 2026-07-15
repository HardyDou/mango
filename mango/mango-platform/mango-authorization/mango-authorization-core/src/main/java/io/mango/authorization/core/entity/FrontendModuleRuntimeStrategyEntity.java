package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 前端能力模块运行策略。
 * <p>
 * 菜单仍属于逻辑应用；该表只决定某个部署配置档下模块页面由本地包还是远程运行单元承载。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_frontend_module_runtime_strategy")
public class FrontendModuleRuntimeStrategyEntity extends AuthorizationBaseEntity {
    /** 策略 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getStrategyId() {
        return getId();
    }

    public void setStrategyId(Long strategyId) {
        setId(strategyId);
    }

    /** 逻辑应用编码。 */
    private String appCode;

    /** 能力模块编码。 */
    private String moduleCode;

    /** 部署配置档：monolith/hybrid/micro。 */
    private String deployProfile;

    /** 页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK。 */
    private String pageType;

    /** 前端运行单元编码，关联 authorization_frontend_app_registry.app_code。 */
    private String runtimeCode;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */

    /** 更新时间。 */
}
