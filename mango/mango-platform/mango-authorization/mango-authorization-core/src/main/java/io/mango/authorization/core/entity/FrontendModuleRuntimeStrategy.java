package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端能力模块运行策略。
 * <p>
 * 菜单仍属于逻辑应用；该表只决定某个部署配置档下模块页面由本地包还是远程运行单元承载。
 */
@Data
@TableName("authorization_frontend_module_runtime_strategy")
public class FrontendModuleRuntimeStrategy implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 策略 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long strategyId;

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
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
